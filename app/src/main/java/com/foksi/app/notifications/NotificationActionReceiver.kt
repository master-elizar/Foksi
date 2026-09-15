package com.foksi.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.ReminderType
import kotlinx.coroutines.launch

/** Handles the buttons on a posted notification. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AppGraph.init(context)
        val pendingResult = goAsync()
        val action = intent.action
        val eventId = intent.getLongExtra(Extras.EXTRA_EVENT_ID, -1L)
        val notificationId = intent.getIntExtra(Extras.EXTRA_NOTIFICATION_ID, 0)
        val scheduleId = intent.getLongExtra(Extras.EXTRA_SCHEDULE_ID, -1L)
        val snoozeMinutes = intent.getIntExtra(Extras.EXTRA_SNOOZE_MINUTES, 10)

        AppGraph.applicationScope.launch {
            try {
                when (action) {
                    Extras.ACTION_DONE -> {
                        AlarmService.stop(context)
                        AlarmSoundPlayer.stop()
                        if (eventId > 0) AppGraph.planInteractor.setCompleted(eventId, true)
                        AppGraph.notifier.cancel(notificationId)
                    }

                    Extras.ACTION_SNOOZE -> {
                        AlarmService.stop(context)
                        AlarmSoundPlayer.stop()
                        val scheduled = if (scheduleId > 0) AppGraph.planRepository.getScheduled(scheduleId) else null
                        val occurrence = scheduled?.occurrenceStart
                            ?: AppGraph.planRepository.getItem(eventId)?.startAt
                            ?: TimeUtils.now()
                        AppGraph.scheduler.snooze(
                            eventId = eventId,
                            occurrenceStart = occurrence,
                            triggerAt = TimeUtils.now() + snoozeMinutes * 60_000L,
                            type = scheduled?.type ?: ReminderType.NORMAL,
                        )
                        AppGraph.notifier.cancel(notificationId)
                    }

                    Extras.ACTION_DISMISS, Extras.ACTION_ALARM_STOP -> {
                        AlarmService.stop(context)
                        AlarmSoundPlayer.stop()
                        AppGraph.notifier.cancel(notificationId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
