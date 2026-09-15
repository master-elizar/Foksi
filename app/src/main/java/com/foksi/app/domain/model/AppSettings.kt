package com.foksi.app.domain.model

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class PaletteId { UBUNTU, LAVENDER, MINT, SKY, ROSE }

enum class TextSize(val scale: Float) {
    SMALL(0.9f), NORMAL(1.0f), LARGE(1.12f), HUGE(1.25f)
}

data class AppSettings(
    val onboardingDone: Boolean = false,

    val theme: ThemeMode = ThemeMode.SYSTEM,
    val palette: PaletteId = PaletteId.UBUNTU,
    val textSize: TextSize = TextSize.NORMAL,

    val notificationsEnabled: Boolean = true,
    val normalEnabled: Boolean = true,
    val importantEnabled: Boolean = true,
    val alarmsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lockScreenEnabled: Boolean = true,
    val notificationSoundUri: String = "",
    val alarmSoundUri: String = "",

    val quietHoursEnabled: Boolean = true,
    val quietFromMinutes: Int = 22 * 60,
    val quietToMinutes: Int = 8 * 60,
    val quietAllowImportant: Boolean = false,
    val quietAllowAlarms: Boolean = true,

    val advanceDefaultEnabled: Boolean = false,
    val advanceDefaultPerWeek: Int = 3,
    val minGapHours: Int = 6,

    val firstDayOfWeek: Int = 1,
    val use24h: Boolean = true,
    val defaultDurationMinutes: Int = 60,

    val widgetEventCount: Int = 6,
    val widgetShowNotes: Boolean = true,
    val widgetShowCompleted: Boolean = false,
    val widgetShowCountdown: Boolean = true,
)
