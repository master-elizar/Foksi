@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.notifications

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.ui.components.FoksiDatePickerDialog
import com.foksi.app.ui.components.FoksiTimePickerDialog
import com.foksi.app.ui.components.SnoozeOptionsDialog
import com.foksi.app.ui.components.collectSettings
import com.foksi.app.ui.theme.FoksiTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** A lightweight picker shown from the "Snooze" notification action. */
class SnoozeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(this)

        val eventId = intent.getLongExtra(Extras.EXTRA_EVENT_ID, -1L)
        val notificationId = intent.getIntExtra(Extras.EXTRA_NOTIFICATION_ID, 0)
        val scheduleId = intent.getLongExtra(Extras.EXTRA_SCHEDULE_ID, -1L)
        val occurrence = intent.getLongExtra(Extras.EXTRA_OCCURRENCE, TimeUtils.now())

        setContent {
            val settings by collectSettings()
            FoksiTheme(settings) {
                var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
                var showDate by remember { mutableStateOf(false) }
                var showTime by remember { mutableStateOf(false) }

                SnoozeOptionsDialog(
                    onDismiss = { finish() },
                    onPicked = { option ->
                        val trigger = option.triggerAt()
                        if (trigger == null) {
                            showDate = true
                        } else {
                            apply(eventId, notificationId, scheduleId, occurrence, trigger)
                        }
                    }
                )

                if (showDate) {
                    FoksiDatePickerDialog(
                        initialMillis = TimeUtils.now(),
                        onDismiss = { showDate = false },
                        onPicked = { date ->
                            pickedDate = date
                            showTime = true
                        }
                    )
                }
                if (showTime) {
                    FoksiTimePickerDialog(
                        initialMinuteOfDay = TimeUtils.minutesOfDay(TimeUtils.now()) + 30,
                        use24h = settings.use24h,
                        onDismiss = { showTime = false },
                        onPicked = { time: LocalTime ->
                            val date = pickedDate ?: TimeUtils.toLocalDate(TimeUtils.now())
                            apply(
                                eventId, notificationId, scheduleId, occurrence,
                                TimeUtils.toMillis(date, time)
                            )
                        }
                    )
                }
            }
        }
    }

    private fun apply(
        eventId: Long,
        notificationId: Int,
        scheduleId: Long,
        occurrence: Long,
        triggerAt: Long,
    ) {
        lifecycleScope.launch {
            AlarmSoundPlayer.stop()
            val type = if (scheduleId > 0) {
                AppGraph.planRepository.getScheduled(scheduleId)?.type ?: ReminderType.NORMAL
            } else {
                ReminderType.NORMAL
            }
            AppGraph.scheduler.snooze(eventId, occurrence, triggerAt.coerceAtLeast(TimeUtils.now() + 60_000), type)
            AppGraph.notifier.cancel(notificationId)
            finish()
        }
    }
}
