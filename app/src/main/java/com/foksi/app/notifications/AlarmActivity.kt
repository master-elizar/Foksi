@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.notifications

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.foksi.app.MainActivity
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.ui.components.SnoozeOptionsDialog
import com.foksi.app.ui.components.collectSettings
import com.foksi.app.ui.theme.FoksiTheme
import kotlinx.coroutines.launch

/**
 * The full-screen alarm. Shown over the lock screen for reminders the user marked as an alarm,
 * with the three actions the situation calls for: open, snooze, confirm.
 */
class AlarmActivity : ComponentActivity() {

    private var notificationId: Int = 0
    private var eventId: Long = -1
    private var scheduleId: Long = -1
    private var occurrence: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(this)
        showOverLockScreen()

        eventId = intent.getLongExtra(Extras.EXTRA_EVENT_ID, -1L)
        notificationId = intent.getIntExtra(Extras.EXTRA_NOTIFICATION_ID, 0)
        scheduleId = intent.getLongExtra(Extras.EXTRA_SCHEDULE_ID, -1L)
        occurrence = intent.getLongExtra(Extras.EXTRA_OCCURRENCE, TimeUtils.now())

        setContent {
            val settings by collectSettings()
            var details by remember { mutableStateOf<PlanItemDetails?>(null) }
            var showSnooze by remember { mutableStateOf(false) }

            LaunchedEffect(eventId) {
                details = AppGraph.planRepository.getDetails(eventId)
                val current = AppGraph.settingsRepository.current()
                // Normally AlarmService already rings; this only covers the fallback path where
                // the system refused to start a foreground service.
                if (current.alarmsEnabled && !AlarmSoundPlayer.isRinging) {
                    AlarmSoundPlayer.start(
                        context = this@AlarmActivity,
                        uri = NotificationChannels.soundFor(isAlarm = true, settings = current),
                        vibrate = current.vibrationEnabled,
                        sound = current.soundEnabled,
                    )
                }
            }

            FoksiTheme(settings) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = TimeUtils.formatTime(this@AlarmActivity, occurrence, settings.use24h),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = details?.item?.title.orEmpty(),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = TimeUtils.formatDate(this@AlarmActivity, occurrence),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        details?.item?.location?.takeIf { it.isNotBlank() }?.let { place ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = place,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(40.dp))

                        Button(
                            onClick = { openEvent() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors()
                        ) { Text(stringResource(R.string.alarm_open_event)) }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showSnooze = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.alarm_snooze)) }

                        Spacer(Modifier.height(12.dp))

                        TextButton(
                            onClick = { confirm() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.alarm_confirm)) }
                    }

                    if (showSnooze) {
                        SnoozeOptionsDialog(
                            onDismiss = { showSnooze = false },
                            onPicked = { option ->
                                val trigger = option.triggerAt() ?: (TimeUtils.now() + 10 * 60_000L)
                                snooze(trigger)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun openEvent() {
        stopRinging()
        AppGraph.notifier.cancel(notificationId)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = Extras.ACTION_OPEN_EVENT
                putExtra(Extras.EXTRA_EVENT_ID, eventId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )
        finish()
    }

    private fun confirm() {
        stopRinging()
        AppGraph.notifier.cancel(notificationId)
        finish()
    }

    private fun snooze(triggerAt: Long) {
        lifecycleScope.launch {
            stopRinging()
            AppGraph.scheduler.snooze(eventId, occurrence, triggerAt, ReminderType.ALARM)
            AppGraph.notifier.cancel(notificationId)
            finish()
        }
    }

    private fun stopRinging() {
        AlarmService.stop(this)
        AlarmSoundPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
    }
}
