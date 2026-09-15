package com.foksi.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.foksi.app.di.AppGraph
import kotlinx.coroutines.launch

/** Entry point for every alarm the scheduler registers with the system. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AppGraph.init(context)
        val pendingResult = goAsync()
        val action = intent.action
        val scheduleId = intent.getLongExtra(Extras.EXTRA_SCHEDULE_ID, -1L)
        AppGraph.applicationScope.launch {
            try {
                when (action) {
                    Extras.ACTION_FIRE -> if (scheduleId > 0) ReminderFiring.fire(context, scheduleId)
                    Extras.ACTION_RESYNC -> AppGraph.scheduler.rescheduleAll()
                }
                ReminderFiring.fireDue(context)
                AppGraph.scheduler.syncAlarms()
            } catch (e: Exception) {
                Log.e("FoksiAlarm", "Failed to handle $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
