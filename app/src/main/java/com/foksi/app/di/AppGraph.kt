package com.foksi.app.di

import android.content.Context
import com.foksi.app.data.backup.BackupManager
import com.foksi.app.data.local.FoksiDatabase
import com.foksi.app.data.repository.PlanRepository
import com.foksi.app.data.repository.SettingsRepository
import com.foksi.app.domain.usecase.PlanInteractor
import com.foksi.app.notifications.Notifier
import com.foksi.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * A tiny hand-written service locator. The app has a single process and a handful of singletons,
 * so this keeps startup instant and the dependency wiring obvious.
 */
object AppGraph {

    @Volatile
    private var appContext: Context? = null

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(context: Context) {
        if (appContext == null) {
            synchronized(this) {
                if (appContext == null) appContext = context.applicationContext
            }
        }
    }

    private fun requireContext(): Context =
        appContext ?: error("AppGraph.init() must be called from Application.onCreate()")

    val database: FoksiDatabase by lazy { FoksiDatabase.get(requireContext()) }

    val planRepository: PlanRepository by lazy { PlanRepository(database) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(requireContext()) }

    val notifier: Notifier by lazy { Notifier(requireContext()) }

    val scheduler: ReminderScheduler by lazy {
        ReminderScheduler(requireContext(), planRepository, settingsRepository)
    }

    val planInteractor: PlanInteractor by lazy {
        PlanInteractor(requireContext(), planRepository, scheduler)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(requireContext(), planRepository)
    }

    /** Application context for the few callers that legitimately need one. */
    fun notifierContext(): Context = requireContext()
}
