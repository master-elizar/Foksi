package com.foksi.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.foksi.app.di.AppGraph
import com.foksi.app.widgets.WidgetUpdater
import java.util.concurrent.TimeUnit

/**
 * Safety net. Exact alarms are the primary mechanism; this periodic job simply makes sure that a
 * dropped alarm (aggressive battery saver, OEM task killer) is noticed within a few hours.
 */
class ReminderSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        AppGraph.init(applicationContext)
        ReminderFiring.fireDue(applicationContext)
        AppGraph.scheduler.rescheduleAll()
        WidgetUpdater.requestUpdate(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val NAME = "foksi_reminder_sync"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
