package com.foksi.app.notifications

import android.content.Context
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.logic.QuietHours
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.domain.model.ScheduledNotification

/**
 * Decides what actually happens when a scheduled reminder becomes due. Kept separate from the
 * broadcast receiver so both the alarm path and the WorkManager safety net share one behaviour.
 */
object ReminderFiring {

    /** Anything that slipped through (device was off, alarm dropped) is caught up here. */
    suspend fun fireDue(context: Context) {
        val now = TimeUtils.now()
        AppGraph.planRepository.getDue(now).forEach { fire(context, it.id) }
    }

    suspend fun fire(context: Context, scheduleId: Long) {
        val repository = AppGraph.planRepository
        val scheduled = repository.getScheduled(scheduleId) ?: return
        if (scheduled.fired) return

        val settings = AppGraph.settingsRepository.current()
        val details = repository.getDetails(scheduled.eventId)
        if (details == null) {
            repository.deleteScheduled(scheduleId)
            return
        }
        if (details.item.completed) {
            repository.markFired(scheduleId)
            return
        }
        if (!settings.notificationsEnabled || !typeAllowed(scheduled.type, settings.normalEnabled, settings.importantEnabled, settings.alarmsEnabled)) {
            repository.markFired(scheduleId)
            return
        }

        val now = TimeUtils.now()
        // A reminder that was silenced by quiet hours is moved rather than dropped, except for
        // the purely optional random nudges which simply disappear for the night.
        if (QuietHours.blocks(settings, scheduled.type, now)) {
            if (scheduled.kind == NotificationKind.RANDOM) {
                repository.markFired(scheduleId)
            } else {
                val shifted = QuietHours.shiftOutOfQuiet(settings, now)
                val limit = scheduled.occurrenceStart
                if (shifted < limit || scheduled.kind == NotificationKind.MAIN) {
                    repository.updateTrigger(scheduleId, shifted)
                } else {
                    repository.markFired(scheduleId)
                }
            }
            AppGraph.scheduler.syncAlarms(settings)
            return
        }

        val openChecklist = repository.openChecklistCount(scheduled.eventId)
        if (scheduled.kind == NotificationKind.CHECKLIST && openChecklist == 0) {
            repository.markFired(scheduleId)
            return
        }

        // An alarm-type reminder is handed to a foreground service so it can hold the CPU awake,
        // keep ringing and put the alarm screen over the lock screen. Everything else is a plain
        // notification.
        val ringingAsAlarm = scheduled.type == ReminderType.ALARM &&
            AlarmService.start(context, scheduled.id)
        if (!ringingAsAlarm) {
            AppGraph.notifier.post(scheduled, details, settings, openChecklist)
        }
        repository.markFired(scheduleId)

        scheduleFollowUpIfNeeded(scheduled, details.reminders)
        AppGraph.scheduler.syncAlarms(settings)
    }

    private suspend fun scheduleFollowUpIfNeeded(
        scheduled: ScheduledNotification,
        reminders: List<com.foksi.app.domain.model.Reminder>,
    ) {
        val reminderId = scheduled.reminderId ?: return
        val reminder = reminders.firstOrNull { it.id == reminderId } ?: return
        if (!reminder.repeatUntilAck) return
        val nextIndex = scheduled.repeatIndex + 1
        if (nextIndex > reminder.repeatMaxCount) return
        AppGraph.scheduler.scheduleRepeatFollowUp(
            scheduled, reminder.repeatIntervalMinutes.coerceAtLeast(1), nextIndex
        )
    }

    private fun typeAllowed(
        type: ReminderType,
        normal: Boolean,
        important: Boolean,
        alarms: Boolean,
    ): Boolean = when (type) {
        ReminderType.NORMAL -> normal
        ReminderType.IMPORTANT -> important
        ReminderType.ALARM -> alarms
    }
}
