package com.foksi.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val items: List<BackupItem> = emptyList(),
)

@Serializable
data class BackupItem(
    val type: String = "EVENT",
    val title: String = "",
    val description: String = "",
    val notes: String = "",
    val startAt: Long? = null,
    val allDay: Boolean = false,
    val durationMinutes: Int = 60,
    val location: String = "",
    val url: String = "",
    val priority: String = "NORMAL",
    val colorArgb: Int? = null,
    val completed: Boolean = false,
    val categoryName: String? = null,
    val repeatMode: String = "NONE",
    val repeatInterval: Int = 1,
    val repeatWeekDays: List<Int> = emptyList(),
    val repeatUntil: Long? = null,
    val advance: BackupAdvance? = null,
    val reminders: List<BackupReminder> = emptyList(),
    val checklist: List<BackupChecklistItem> = emptyList(),
    val attachments: List<BackupAttachment> = emptyList(),
)

@Serializable
data class BackupReminder(
    val minutesBefore: Int = 15,
    val type: String = "NORMAL",
    val enabled: Boolean = true,
    val repeatUntilAck: Boolean = false,
    val repeatIntervalMinutes: Int = 5,
    val repeatMaxCount: Int = 3,
)

@Serializable
data class BackupAdvance(
    val enabled: Boolean = false,
    val mode: String = "CHAIN",
    val startDaysBefore: Int = 14,
    val intervalDays: Int = 3,
    val perWeek: Int = 3,
    val windowStartMinutes: Int = 540,
    val windowEndMinutes: Int = 1260,
    val workdaysOnly: Boolean = false,
    val disabledChainSteps: List<Int> = emptyList(),
    val checklistNudge: Boolean = false,
)

@Serializable
data class BackupChecklistItem(
    val text: String = "",
    val done: Boolean = false,
)

@Serializable
data class BackupAttachment(
    val uri: String = "",
    val name: String = "",
    val mime: String = "",
    val isLink: Boolean = false,
)
