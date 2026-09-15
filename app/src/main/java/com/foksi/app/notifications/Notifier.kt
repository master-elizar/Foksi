package com.foksi.app.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.foksi.app.MainActivity
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Priority
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.domain.model.ScheduledNotification

/** Builds and posts every notification the app shows. */
class Notifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun areNotificationsEnabled(): Boolean = manager.areNotificationsEnabled()

    fun post(
        scheduled: ScheduledNotification,
        details: PlanItemDetails,
        settings: AppSettings,
        openChecklistItems: Int,
    ) {
        NotificationChannels.ensureChannels(context, settings)
        val item = details.item
        val isAdvance = scheduled.kind in ADVANCE_KINDS
        val channelId = NotificationChannels.channelId(scheduled.type, isAdvance, settings)
        val notificationId = scheduled.id.toInt().let { if (it == 0) item.id.toInt() else it }
        val now = TimeUtils.now()

        val title = NotificationMessages.title(context, scheduled.kind, item, scheduled.occurrenceStart, now)
        val text = NotificationMessages.text(
            context, scheduled.kind, item, scheduled.occurrenceStart,
            settings.use24h, openChecklistItems, now
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(buildBigText(details, text)))
            .setContentIntent(openEventIntent(item.id, notificationId))
            .setAutoCancel(scheduled.type != ReminderType.ALARM)
            .setOnlyAlertOnce(false)
            .setWhen(scheduled.occurrenceStart)
            .setShowWhen(true)
            .setGroup(NotificationChannels.GROUP_KEY)
            .setVisibility(
                if (settings.lockScreenEnabled) NotificationCompat.VISIBILITY_PUBLIC
                else NotificationCompat.VISIBILITY_SECRET
            )
            .setPriority(
                when (scheduled.type) {
                    ReminderType.ALARM -> NotificationCompat.PRIORITY_MAX
                    ReminderType.IMPORTANT -> NotificationCompat.PRIORITY_HIGH
                    ReminderType.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setCategory(
                if (scheduled.type == ReminderType.ALARM) NotificationCompat.CATEGORY_ALARM
                else NotificationCompat.CATEGORY_REMINDER
            )

        if (item.priority == Priority.CRITICAL) {
            builder.setColorized(false)
        }

        builder.addAction(
            0, context.getString(R.string.notif_action_open), openEventIntent(item.id, notificationId)
        )
        builder.addAction(
            0, context.getString(R.string.notif_action_snooze),
            snoozePickerIntent(item.id, notificationId, scheduled)
        )
        builder.addAction(
            0, context.getString(R.string.notif_action_done),
            actionIntent(Extras.ACTION_DONE, item.id, notificationId, scheduled.id)
        )

        if (scheduled.type == ReminderType.ALARM) {
            builder.setOngoing(true)
            builder.setFullScreenIntent(alarmIntent(item.id, notificationId, scheduled), true)
            builder.setDeleteIntent(actionIntent(Extras.ACTION_ALARM_STOP, item.id, notificationId, scheduled.id))
        }

        if (!settings.soundEnabled) builder.setSilent(true)

        runCatching { manager.notify(notificationId, builder.build()) }
    }

    private fun buildBigText(details: PlanItemDetails, text: String): String {
        val extras = buildList {
            add(text)
            if (details.item.notes.isNotBlank()) add(details.item.notes.take(400))
            if (details.checklist.isNotEmpty()) {
                add(
                    context.getString(
                        R.string.checklist_progress, details.checklistDone, details.checklistTotal
                    )
                )
            }
        }
        return extras.joinToString("\n")
    }

    fun cancel(notificationId: Int) = manager.cancel(notificationId)

    fun cancelAll() = manager.cancelAll()

    // --- intents --------------------------------------------------------------------------------

    private fun flags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun openEventIntent(eventId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Extras.ACTION_OPEN_EVENT
            putExtra(Extras.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, requestCode * 31 + 1, intent, flags())
    }

    private fun snoozePickerIntent(
        eventId: Long,
        notificationId: Int,
        scheduled: ScheduledNotification,
    ): PendingIntent {
        val intent = Intent(context, SnoozeActivity::class.java).apply {
            putExtra(Extras.EXTRA_EVENT_ID, eventId)
            putExtra(Extras.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(Extras.EXTRA_SCHEDULE_ID, scheduled.id)
            putExtra(Extras.EXTRA_OCCURRENCE, scheduled.occurrenceStart)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, notificationId * 31 + 2, intent, flags())
    }

    private fun alarmIntent(
        eventId: Long,
        notificationId: Int,
        scheduled: ScheduledNotification,
    ): PendingIntent {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(Extras.EXTRA_EVENT_ID, eventId)
            putExtra(Extras.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(Extras.EXTRA_SCHEDULE_ID, scheduled.id)
            putExtra(Extras.EXTRA_OCCURRENCE, scheduled.occurrenceStart)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, notificationId * 31 + 3, intent, flags())
    }

    private fun actionIntent(
        action: String,
        eventId: Long,
        notificationId: Int,
        scheduleId: Long,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(Extras.EXTRA_EVENT_ID, eventId)
            putExtra(Extras.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(Extras.EXTRA_SCHEDULE_ID, scheduleId)
        }
        return PendingIntent.getBroadcast(context, notificationId * 31 + action.hashCode(), intent, flags())
    }

    companion object {
        val ADVANCE_KINDS = setOf(
            NotificationKind.CHAIN,
            NotificationKind.INTERVAL,
            NotificationKind.RANDOM,
            NotificationKind.CHECKLIST,
        )

        fun supportsFullScreen(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        @Suppress("unused")
        fun defaultFlags(): Int = Notification.FLAG_AUTO_CANCEL
    }
}
