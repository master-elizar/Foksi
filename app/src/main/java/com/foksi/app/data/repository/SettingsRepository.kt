package com.foksi.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.PaletteId
import com.foksi.app.domain.model.TextSize
import com.foksi.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "foksi_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val theme = stringPreferencesKey("theme")
        val palette = stringPreferencesKey("palette")
        val textSize = stringPreferencesKey("text_size")

        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val normalEnabled = booleanPreferencesKey("normal_enabled")
        val importantEnabled = booleanPreferencesKey("important_enabled")
        val alarmsEnabled = booleanPreferencesKey("alarms_enabled")
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val vibrationEnabled = booleanPreferencesKey("vibration_enabled")
        val lockScreenEnabled = booleanPreferencesKey("lockscreen_enabled")
        val notificationSound = stringPreferencesKey("notification_sound")
        val alarmSound = stringPreferencesKey("alarm_sound")

        val quietEnabled = booleanPreferencesKey("quiet_enabled")
        val quietFrom = intPreferencesKey("quiet_from")
        val quietTo = intPreferencesKey("quiet_to")
        val quietImportant = booleanPreferencesKey("quiet_important")
        val quietAlarms = booleanPreferencesKey("quiet_alarms")

        val advanceDefault = booleanPreferencesKey("advance_default")
        val advancePerWeek = intPreferencesKey("advance_per_week")
        val minGapHours = intPreferencesKey("min_gap_hours")

        val firstDayOfWeek = intPreferencesKey("first_day_of_week")
        val use24h = booleanPreferencesKey("use_24h")
        val defaultDuration = intPreferencesKey("default_duration")

        val widgetCount = intPreferencesKey("widget_count")
        val widgetNotes = booleanPreferencesKey("widget_notes")
        val widgetCompleted = booleanPreferencesKey("widget_completed")
        val widgetCountdown = booleanPreferencesKey("widget_countdown")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            onboardingDone = this[Keys.onboardingDone] ?: defaults.onboardingDone,
            theme = this[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.theme,
            palette = this[Keys.palette]?.let { runCatching { PaletteId.valueOf(it) }.getOrNull() } ?: defaults.palette,
            textSize = this[Keys.textSize]?.let { runCatching { TextSize.valueOf(it) }.getOrNull() } ?: defaults.textSize,
            notificationsEnabled = this[Keys.notificationsEnabled] ?: defaults.notificationsEnabled,
            normalEnabled = this[Keys.normalEnabled] ?: defaults.normalEnabled,
            importantEnabled = this[Keys.importantEnabled] ?: defaults.importantEnabled,
            alarmsEnabled = this[Keys.alarmsEnabled] ?: defaults.alarmsEnabled,
            soundEnabled = this[Keys.soundEnabled] ?: defaults.soundEnabled,
            vibrationEnabled = this[Keys.vibrationEnabled] ?: defaults.vibrationEnabled,
            lockScreenEnabled = this[Keys.lockScreenEnabled] ?: defaults.lockScreenEnabled,
            notificationSoundUri = this[Keys.notificationSound] ?: defaults.notificationSoundUri,
            alarmSoundUri = this[Keys.alarmSound] ?: defaults.alarmSoundUri,
            quietHoursEnabled = this[Keys.quietEnabled] ?: defaults.quietHoursEnabled,
            quietFromMinutes = this[Keys.quietFrom] ?: defaults.quietFromMinutes,
            quietToMinutes = this[Keys.quietTo] ?: defaults.quietToMinutes,
            quietAllowImportant = this[Keys.quietImportant] ?: defaults.quietAllowImportant,
            quietAllowAlarms = this[Keys.quietAlarms] ?: defaults.quietAllowAlarms,
            advanceDefaultEnabled = this[Keys.advanceDefault] ?: defaults.advanceDefaultEnabled,
            advanceDefaultPerWeek = this[Keys.advancePerWeek] ?: defaults.advanceDefaultPerWeek,
            minGapHours = this[Keys.minGapHours] ?: defaults.minGapHours,
            firstDayOfWeek = this[Keys.firstDayOfWeek] ?: defaults.firstDayOfWeek,
            use24h = this[Keys.use24h] ?: defaults.use24h,
            defaultDurationMinutes = this[Keys.defaultDuration] ?: defaults.defaultDurationMinutes,
            widgetEventCount = this[Keys.widgetCount] ?: defaults.widgetEventCount,
            widgetShowNotes = this[Keys.widgetNotes] ?: defaults.widgetShowNotes,
            widgetShowCompleted = this[Keys.widgetCompleted] ?: defaults.widgetShowCompleted,
            widgetShowCountdown = this[Keys.widgetCountdown] ?: defaults.widgetShowCountdown,
        )
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setOnboardingDone(value: Boolean) = edit { it[Keys.onboardingDone] = value }
    suspend fun setTheme(value: ThemeMode) = edit { it[Keys.theme] = value.name }
    suspend fun setPalette(value: PaletteId) = edit { it[Keys.palette] = value.name }
    suspend fun setTextSize(value: TextSize) = edit { it[Keys.textSize] = value.name }

    suspend fun setNotificationsEnabled(value: Boolean) = edit { it[Keys.notificationsEnabled] = value }
    suspend fun setNormalEnabled(value: Boolean) = edit { it[Keys.normalEnabled] = value }
    suspend fun setImportantEnabled(value: Boolean) = edit { it[Keys.importantEnabled] = value }
    suspend fun setAlarmsEnabled(value: Boolean) = edit { it[Keys.alarmsEnabled] = value }
    suspend fun setSoundEnabled(value: Boolean) = edit { it[Keys.soundEnabled] = value }
    suspend fun setVibrationEnabled(value: Boolean) = edit { it[Keys.vibrationEnabled] = value }
    suspend fun setLockScreenEnabled(value: Boolean) = edit { it[Keys.lockScreenEnabled] = value }
    suspend fun setNotificationSound(uri: String) = edit { it[Keys.notificationSound] = uri }
    suspend fun setAlarmSound(uri: String) = edit { it[Keys.alarmSound] = uri }

    suspend fun setQuietEnabled(value: Boolean) = edit { it[Keys.quietEnabled] = value }
    suspend fun setQuietFrom(minutes: Int) = edit { it[Keys.quietFrom] = minutes }
    suspend fun setQuietTo(minutes: Int) = edit { it[Keys.quietTo] = minutes }
    suspend fun setQuietAllowImportant(value: Boolean) = edit { it[Keys.quietImportant] = value }
    suspend fun setQuietAllowAlarms(value: Boolean) = edit { it[Keys.quietAlarms] = value }

    suspend fun setAdvanceDefault(value: Boolean) = edit { it[Keys.advanceDefault] = value }
    suspend fun setAdvancePerWeek(value: Int) = edit { it[Keys.advancePerWeek] = value }
    suspend fun setMinGapHours(value: Int) = edit { it[Keys.minGapHours] = value }

    suspend fun setFirstDayOfWeek(value: Int) = edit { it[Keys.firstDayOfWeek] = value }
    suspend fun setUse24h(value: Boolean) = edit { it[Keys.use24h] = value }
    suspend fun setDefaultDuration(value: Int) = edit { it[Keys.defaultDuration] = value }

    suspend fun setWidgetCount(value: Int) = edit { it[Keys.widgetCount] = value }
    suspend fun setWidgetNotes(value: Boolean) = edit { it[Keys.widgetNotes] = value }
    suspend fun setWidgetCompleted(value: Boolean) = edit { it[Keys.widgetCompleted] = value }
    suspend fun setWidgetCountdown(value: Boolean) = edit { it[Keys.widgetCountdown] = value }
}
