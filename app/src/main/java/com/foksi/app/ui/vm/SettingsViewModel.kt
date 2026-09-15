package com.foksi.app.ui.vm

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foksi.app.R
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.PaletteId
import com.foksi.app.domain.model.TextSize
import com.foksi.app.domain.model.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsMessage(val textRes: Int, val arg: Int? = null)

class SettingsViewModel : ViewModel() {

    private val repository = AppGraph.settingsRepository
    private val backup = AppGraph.backupManager
    private val interactor = AppGraph.planInteractor

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val messages = Channel<SettingsMessage>(Channel.BUFFERED)
    val events: Flow<SettingsMessage> = messages.receiveAsFlow()

    private fun edit(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun editAndReschedule(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            interactor.rescheduleEverything()
        }
    }

    fun setTheme(value: ThemeMode) = edit { repository.setTheme(value) }
    fun setPalette(value: PaletteId) = edit { repository.setPalette(value) }
    fun setTextSize(value: TextSize) = edit { repository.setTextSize(value) }

    fun setNotificationsEnabled(value: Boolean) = editAndReschedule { repository.setNotificationsEnabled(value) }
    fun setNormalEnabled(value: Boolean) = edit { repository.setNormalEnabled(value) }
    fun setImportantEnabled(value: Boolean) = edit { repository.setImportantEnabled(value) }
    fun setAlarmsEnabled(value: Boolean) = edit { repository.setAlarmsEnabled(value) }
    fun setSoundEnabled(value: Boolean) = edit { repository.setSoundEnabled(value) }
    fun setVibrationEnabled(value: Boolean) = edit { repository.setVibrationEnabled(value) }
    fun setLockScreenEnabled(value: Boolean) = edit { repository.setLockScreenEnabled(value) }
    fun setNotificationSound(uri: String) = edit { repository.setNotificationSound(uri) }
    fun setAlarmSound(uri: String) = edit { repository.setAlarmSound(uri) }

    fun setQuietEnabled(value: Boolean) = editAndReschedule { repository.setQuietEnabled(value) }
    fun setQuietFrom(minutes: Int) = editAndReschedule { repository.setQuietFrom(minutes) }
    fun setQuietTo(minutes: Int) = editAndReschedule { repository.setQuietTo(minutes) }
    fun setQuietAllowImportant(value: Boolean) = edit { repository.setQuietAllowImportant(value) }
    fun setQuietAllowAlarms(value: Boolean) = edit { repository.setQuietAllowAlarms(value) }

    fun setAdvanceDefault(value: Boolean) = edit { repository.setAdvanceDefault(value) }
    fun setAdvancePerWeek(value: Int) = edit { repository.setAdvancePerWeek(value) }
    fun setMinGapHours(value: Int) = editAndReschedule { repository.setMinGapHours(value) }

    fun setFirstDayOfWeek(value: Int) = edit { repository.setFirstDayOfWeek(value) }
    fun setUse24h(value: Boolean) = edit { repository.setUse24h(value) }
    fun setDefaultDuration(value: Int) = edit { repository.setDefaultDuration(value) }

    fun setWidgetCount(value: Int) = edit {
        repository.setWidgetCount(value)
        com.foksi.app.widgets.WidgetUpdater.requestUpdate(AppGraph.notifierContext())
    }

    fun setWidgetNotes(value: Boolean) = edit {
        repository.setWidgetNotes(value)
        com.foksi.app.widgets.WidgetUpdater.requestUpdate(AppGraph.notifierContext())
    }

    fun setWidgetCompleted(value: Boolean) = edit {
        repository.setWidgetCompleted(value)
        com.foksi.app.widgets.WidgetUpdater.requestUpdate(AppGraph.notifierContext())
    }

    fun setWidgetCountdown(value: Boolean) = edit {
        repository.setWidgetCountdown(value)
        com.foksi.app.widgets.WidgetUpdater.requestUpdate(AppGraph.notifierContext())
    }

    fun exportJson(uri: Uri) = viewModelScope.launch {
        runCatching { backup.exportJson(uri) }
            .onSuccess { messages.send(SettingsMessage(R.string.export_success, it)) }
            .onFailure { messages.send(SettingsMessage(R.string.export_failed)) }
    }

    fun exportIcs(uri: Uri) = viewModelScope.launch {
        runCatching { backup.exportIcs(uri) }
            .onSuccess { messages.send(SettingsMessage(R.string.export_success, it)) }
            .onFailure { messages.send(SettingsMessage(R.string.export_failed)) }
    }

    fun import(uri: Uri) = viewModelScope.launch {
        runCatching { backup.import(uri) }
            .onSuccess {
                interactor.rescheduleEverything()
                messages.send(SettingsMessage(R.string.import_success, it))
            }
            .onFailure { messages.send(SettingsMessage(R.string.import_failed)) }
    }

    fun createBackup() = viewModelScope.launch {
        runCatching { backup.backupToInternal() }
            .onSuccess { messages.send(SettingsMessage(R.string.backup_success)) }
            .onFailure { messages.send(SettingsMessage(R.string.export_failed)) }
    }

    fun restoreBackup() = viewModelScope.launch {
        runCatching { backup.restoreFromInternal() }
            .onSuccess {
                interactor.rescheduleEverything()
                messages.send(SettingsMessage(R.string.restore_success))
            }
            .onFailure { messages.send(SettingsMessage(R.string.import_failed)) }
    }

    fun hasBackup(): Boolean = backup.hasInternalBackup()
}
