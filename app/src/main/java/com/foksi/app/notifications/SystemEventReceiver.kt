package com.foksi.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import androidx.core.content.getSystemService
import com.foksi.app.di.AppGraph
import com.foksi.app.widgets.WidgetUpdater
import kotlinx.coroutines.launch

/**
 * Restores every reminder after a reboot, an app update, or any change to the device clock or
 * time zone — the three situations where Android silently drops pending alarms.
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val userManager = context.getSystemService<UserManager>()
        if (userManager?.isUserUnlocked == false) {
            // Storage is still encrypted; BOOT_COMPLETED will arrive once the user unlocks.
            return
        }
        AppGraph.init(context)
        val pendingResult = goAsync()
        AppGraph.applicationScope.launch {
            try {
                AppGraph.scheduler.rescheduleAll()
                ReminderFiring.fireDue(context)
                WidgetUpdater.requestUpdate(context)
                ReminderSyncWorker.enqueuePeriodic(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
