package com.foksi.app.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService
import com.foksi.app.R
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.AppSettings

enum class ReminderStatusLevel { OK, WARNING, ERROR }

data class ReminderStatus(
    val level: ReminderStatusLevel,
    val messageRes: Int,
    val systemIntent: Intent?,
)

/**
 * Turns Android's various notification restrictions into one honest line the user can act on.
 */
object ReminderStatusChecker {

    fun check(context: Context, settings: AppSettings, needsExactAlarm: Boolean = true): ReminderStatus {
        if (!settings.notificationsEnabled) {
            return ReminderStatus(ReminderStatusLevel.ERROR, R.string.status_app_disabled, null)
        }
        if (!AppGraph.notifier.areNotificationsEnabled()) {
            return ReminderStatus(
                ReminderStatusLevel.ERROR,
                R.string.status_notifications_off,
                appNotificationSettings(context)
            )
        }
        if (needsExactAlarm && !AppGraph.scheduler.canScheduleExact()) {
            return ReminderStatus(
                ReminderStatusLevel.WARNING,
                R.string.status_exact_alarm,
                exactAlarmSettings(context)
            )
        }
        if (!ignoresBatteryOptimisation(context)) {
            return ReminderStatus(
                ReminderStatusLevel.WARNING,
                R.string.status_battery,
                batterySettings()
            )
        }
        return ReminderStatus(ReminderStatusLevel.OK, R.string.status_ok, null)
    }

    private fun ignoresBatteryOptimisation(context: Context): Boolean {
        val power = context.getSystemService<PowerManager>() ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun appNotificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun exactAlarmSettings(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            null
        }

    fun batterySettings(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
