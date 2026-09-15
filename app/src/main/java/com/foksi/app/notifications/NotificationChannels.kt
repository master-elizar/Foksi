package com.foksi.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.content.getSystemService
import com.foksi.app.R
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.ReminderType

/**
 * Android does not let an app change an existing channel's sound or vibration, so channels are
 * versioned: when the user changes a notification setting we create a new channel id and drop the
 * stale ones. That keeps the in-app settings authoritative without asking the user to dig through
 * system settings.
 */
object NotificationChannels {

    private const val PREFIX = "foksi_"
    const val GROUP_KEY = "com.foksi.app.REMINDERS"

    fun channelId(type: ReminderType, isAdvance: Boolean, settings: AppSettings): String {
        val base = when {
            type == ReminderType.ALARM -> "alarm"
            type == ReminderType.IMPORTANT -> "important"
            isAdvance -> "advance"
            else -> "normal"
        }
        return "$PREFIX${base}_${signature(settings)}"
    }

    private fun signature(settings: AppSettings): Int {
        var hash = 7
        hash = 31 * hash + settings.notificationSoundUri.hashCode()
        hash = 31 * hash + settings.alarmSoundUri.hashCode()
        hash = 31 * hash + settings.soundEnabled.hashCode()
        hash = 31 * hash + settings.vibrationEnabled.hashCode()
        hash = 31 * hash + settings.lockScreenEnabled.hashCode()
        return hash and 0xFFFF
    }

    fun ensureChannels(context: Context, settings: AppSettings) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val wanted = listOf(
            Triple(
                channelId(ReminderType.NORMAL, false, settings),
                context.getString(R.string.channel_normal_name) to context.getString(R.string.channel_normal_desc),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            Triple(
                channelId(ReminderType.NORMAL, true, settings),
                context.getString(R.string.channel_advance_name) to context.getString(R.string.channel_advance_desc),
                NotificationManager.IMPORTANCE_DEFAULT
            ),
            Triple(
                channelId(ReminderType.IMPORTANT, false, settings),
                context.getString(R.string.channel_important_name) to context.getString(R.string.channel_important_desc),
                NotificationManager.IMPORTANCE_HIGH
            ),
            Triple(
                channelId(ReminderType.ALARM, false, settings),
                context.getString(R.string.channel_alarm_name) to context.getString(R.string.channel_alarm_desc),
                NotificationManager.IMPORTANCE_HIGH
            ),
        )

        val wantedIds = wanted.map { it.first }.toSet()
        manager.notificationChannels
            .filter { it.id.startsWith(PREFIX) && it.id !in wantedIds }
            .forEach { manager.deleteNotificationChannel(it.id) }

        wanted.forEach { (id, names, importance) ->
            if (manager.getNotificationChannel(id) != null) return@forEach
            val isAlarm = id.startsWith("${PREFIX}alarm")
            val channel = NotificationChannel(id, names.first, importance).apply {
                description = names.second
                enableVibration(settings.vibrationEnabled)
                if (settings.vibrationEnabled) {
                    vibrationPattern = if (isAlarm) {
                        longArrayOf(0, 700, 500, 700, 500, 700)
                    } else {
                        longArrayOf(0, 250, 200, 250)
                    }
                }
                lockscreenVisibility = if (settings.lockScreenEnabled) {
                    android.app.Notification.VISIBILITY_PUBLIC
                } else {
                    android.app.Notification.VISIBILITY_SECRET
                }
                setShowBadge(true)
                if (isAlarm) {
                    setBypassDnd(settings.quietAllowAlarms)
                }
                if (settings.soundEnabled) {
                    val uri = soundFor(isAlarm, settings)
                    val attributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(uri, attributes)
                } else {
                    setSound(null, null)
                }
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun soundFor(isAlarm: Boolean, settings: AppSettings): Uri {
        val custom = if (isAlarm) settings.alarmSoundUri else settings.notificationSoundUri
        if (custom.isNotBlank()) {
            runCatching { return Uri.parse(custom) }
        }
        return if (isAlarm) {
            Settings.System.DEFAULT_ALARM_ALERT_URI ?: Settings.System.DEFAULT_NOTIFICATION_URI
        } else {
            Settings.System.DEFAULT_NOTIFICATION_URI
        }
    }
}
