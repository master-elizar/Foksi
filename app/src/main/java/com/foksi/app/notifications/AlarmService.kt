package com.foksi.app.notifications

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.foksi.app.R
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.ReminderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Makes an alarm-type reminder behave like the system alarm clock.
 *
 * A plain notification is not enough: the process can be frozen, the screen stays off and the
 * ringtone dies with the broadcast. A foreground service keeps the CPU awake, owns the ringtone
 * for as long as the alarm rings, and carries the full-screen intent that puts [AlarmActivity]
 * over the lock screen.
 */
class AlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentScheduleId: Long = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm()
                return START_NOT_STICKY
            }
        }

        val scheduleId = intent?.getLongExtra(Extras.EXTRA_SCHEDULE_ID, -1L) ?: -1L
        currentScheduleId = scheduleId

        // Android gives us a few seconds to become a foreground service, which is less time than
        // a database read may take, so a placeholder goes up first and is replaced right after.
        startForegroundCompat(placeholderNotification())

        scope.launch {
            runCatching { showRealAlarm(scheduleId) }
                .onFailure { Log.e(TAG, "Cannot show alarm $scheduleId", it) }
        }
        return START_STICKY
    }

    private suspend fun showRealAlarm(scheduleId: Long) {
        val repository = AppGraph.planRepository
        val scheduled = repository.getScheduled(scheduleId) ?: return
        val details = repository.getDetails(scheduled.eventId) ?: return
        val settings = AppGraph.settingsRepository.current()
        val openChecklist = repository.openChecklistCount(scheduled.eventId)

        val notification = AppGraph.notifier.buildNotification(
            scheduled = scheduled,
            details = details,
            settings = settings,
            openChecklistItems = openChecklist,
            ongoing = true,
        )
        startForegroundCompat(notification)

        if (settings.alarmsEnabled) {
            AlarmSoundPlayer.start(
                context = this,
                uri = NotificationChannels.soundFor(isAlarm = true, settings = settings),
                vibrate = settings.vibrationEnabled,
                sound = settings.soundEnabled,
            )
        }
    }

    private fun placeholderNotification(): Notification {
        val settings = com.foksi.app.domain.model.AppSettings()
        NotificationChannels.ensureChannels(this, settings)
        return NotificationCompat.Builder(
            this, NotificationChannels.channelId(ReminderType.ALARM, false, settings)
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.channel_alarm_name))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure { Log.e(TAG, "startForeground failed", it) }
    }

    private fun acquireWakeLock() {
        val power = getSystemService<PowerManager>() ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "foksi:alarm").apply {
            setReferenceCounted(false)
            runCatching { acquire(MAX_RING_MILLIS) }
        }
    }

    private fun stopAlarm() {
        AlarmSoundPlayer.stop()
        runCatching { wakeLock?.release() }
        wakeLock = null
        ServiceCompat.stopForegroundRemove(this)
        stopSelf()
    }

    override fun onDestroy() {
        AlarmSoundPlayer.stop()
        runCatching { wakeLock?.release() }
        wakeLock = null
        scope.cancel()
        super.onDestroy()
    }

    private object ServiceCompat {
        fun stopForegroundRemove(service: Service) {
            runCatching {
                androidx.core.app.ServiceCompat.stopForeground(
                    service, androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE
                )
            }
        }
    }

    companion object {
        private const val TAG = "FoksiAlarmService"
        const val ACTION_STOP = "com.foksi.app.action.ALARM_SERVICE_STOP"

        /** Shared with the notification so tapping an action dismisses the very same alarm. */
        const val NOTIFICATION_ID = 424_242

        /** Alarms never ring forever; the wake lock is released at the latest after this. */
        private const val MAX_RING_MILLIS = 10 * 60 * 1000L

        fun start(context: Context, scheduleId: Long): Boolean = runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java)
                    .putExtra(Extras.EXTRA_SCHEDULE_ID, scheduleId)
            )
            true
        }.getOrElse {
            Log.w(TAG, "Foreground alarm service refused, falling back to a notification", it)
            false
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, AlarmService::class.java).setAction(ACTION_STOP)
                )
            }
        }
    }
}
