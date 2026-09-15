package com.foksi.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.foksi.app.core.TimeUtils
import com.foksi.app.data.repository.PlanRepository
import com.foksi.app.data.repository.SettingsRepository
import com.foksi.app.domain.logic.ReminderPlanner
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.domain.model.ScheduledNotification
import com.foksi.app.widgets.WidgetUpdater
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns every alarm the app registers with the system.
 *
 * Strategy: reminders are materialised into the database, and only the ones inside a short
 * horizon are handed to [AlarmManager]. A single re-sync alarm keeps the pipeline moving, which
 * avoids both the per-app alarm limit and pointless background work. WorkManager acts as a safety
 * net in case an exact alarm is dropped by the system.
 */
class ReminderScheduler(
    private val context: Context,
    private val repository: PlanRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val alarmManager = context.getSystemService<AlarmManager>()
    private val mutex = Mutex()

    /** Rebuilds the plan for one item and refreshes the system alarms. */
    suspend fun rescheduleEvent(eventId: Long) {
        val settings = settingsRepository.current()
        val details = repository.getDetails(eventId)
        if (details == null) {
            repository.replacePending(eventId, emptyList())
        } else {
            val planned = ReminderPlanner.plan(details, settings, TimeUtils.now())
            repository.replacePending(eventId, planned)
        }
        syncAlarms(settings)
        WidgetUpdater.requestUpdate(context)
    }

    /** Rebuilds every plan. Used after boot, time changes, settings changes and restores. */
    suspend fun rescheduleAll() {
        val settings = settingsRepository.current()
        val now = TimeUtils.now()
        repository.purgeOldSchedules(now - 7L * 24 * 60 * 60 * 1000)
        val items = repository.getSchedulable()
        for (item in items) {
            val details = repository.getDetails(item.id) ?: continue
            val planned = ReminderPlanner.plan(details, settings, now)
            repository.replacePending(item.id, planned)
        }
        syncAlarms(settings)
        WidgetUpdater.requestUpdate(context)
    }

    /**
     * Fires anything that is already due (for example after the device was off) and registers
     * system alarms for everything inside the horizon.
     */
    suspend fun syncAlarms(settings: AppSettings? = null) = mutex.withLock {
        val effective = settings ?: settingsRepository.current()
        val now = TimeUtils.now()
        val horizonEnd = now + HORIZON_MILLIS
        val pending = repository.getPendingUntil(horizonEnd, MAX_SYSTEM_ALARMS)

        if (effective.notificationsEnabled) pending.forEach { scheduleExact(it) } else cancelAllSystemAlarms(pending)

        val next = repository.getFirstPendingAfter(horizonEnd)
        val resyncAt = minOf(
            next?.triggerAt?.minus(HORIZON_MILLIS / 2) ?: (now + RESYNC_INTERVAL),
            now + RESYNC_INTERVAL
        ).coerceAtLeast(now + 60_000)
        scheduleResync(resyncAt)
    }

    private fun cancelAllSystemAlarms(pending: List<ScheduledNotification>) {
        pending.forEach { cancel(it.id) }
    }

    private fun scheduleExact(item: ScheduledNotification) {
        val manager = alarmManager ?: return
        val pendingIntent = firePendingIntent(item.id)
        val triggerAt = item.triggerAt
        try {
            when {
                item.type == ReminderType.ALARM && canScheduleExact() -> {
                    manager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerAt, openAppPendingIntent()),
                        pendingIntent
                    )
                }

                canScheduleExact() -> manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )

                else -> manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact", e)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun scheduleResync(triggerAt: Long) {
        val manager = alarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = Extras.ACTION_RESYNC }
        val pendingIntent = PendingIntent.getBroadcast(
            context, RESYNC_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(scheduleId: Long) {
        alarmManager?.cancel(firePendingIntent(scheduleId))
    }

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    private fun firePendingIntent(scheduleId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Extras.ACTION_FIRE
            data = android.net.Uri.parse("foksi://reminder/$scheduleId")
            putExtra(Extras.EXTRA_SCHEDULE_ID, scheduleId)
        }
        return PendingIntent.getBroadcast(
            context, scheduleId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(context, com.foksi.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Creates a one-off reminder some minutes from now (snooze). */
    suspend fun snooze(eventId: Long, occurrenceStart: Long, triggerAt: Long, type: ReminderType) {
        val id = repository.insertScheduled(
            ScheduledNotification(
                eventId = eventId,
                triggerAt = triggerAt,
                occurrenceStart = occurrenceStart,
                kind = NotificationKind.SNOOZE,
                type = type,
            )
        )
        val stored = repository.getScheduled(id) ?: return
        scheduleExact(stored)
    }

    /** Schedules the "repeat until confirmed" follow-up of an important reminder. */
    suspend fun scheduleRepeatFollowUp(source: ScheduledNotification, intervalMinutes: Int, index: Int) {
        val triggerAt = TimeUtils.now() + intervalMinutes * 60_000L
        val id = repository.insertScheduled(
            source.copy(
                id = 0,
                triggerAt = triggerAt,
                kind = NotificationKind.REPEAT,
                repeatIndex = index,
                fired = false,
            )
        )
        val stored = repository.getScheduled(id) ?: return
        scheduleExact(stored)
    }

    companion object {
        private const val TAG = "FoksiScheduler"
        private const val RESYNC_REQUEST_CODE = 987_001
        private const val MAX_SYSTEM_ALARMS = 180
        private val HORIZON_MILLIS = 48L * 60 * 60 * 1000
        private val RESYNC_INTERVAL = 6L * 60 * 60 * 1000
    }
}
