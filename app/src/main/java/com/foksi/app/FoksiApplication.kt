package com.foksi.app

import android.app.Application
import androidx.work.Configuration
import com.foksi.app.di.AppGraph
import com.foksi.app.notifications.NotificationChannels
import com.foksi.app.notifications.ReminderFiring
import com.foksi.app.notifications.ReminderSyncWorker
import com.foksi.app.widgets.WidgetUpdater
import kotlinx.coroutines.launch

class FoksiApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)

        AppGraph.applicationScope.launch {
            val settings = AppGraph.settingsRepository.current()
            NotificationChannels.ensureChannels(this@FoksiApplication, settings)
            // Catch up on anything that became due while the app was not running, then make sure
            // the alarm pipeline is primed.
            ReminderFiring.fireDue(this@FoksiApplication)
            AppGraph.scheduler.rescheduleAll()
            ReminderSyncWorker.enqueuePeriodic(this@FoksiApplication)
            WidgetUpdater.requestUpdate(this@FoksiApplication)
        }
    }
}
