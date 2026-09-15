package com.foksi.app.domain.model

/** Kind of thing the user stored: a calendar event, a to-do or a free-form note. */
enum class ItemType { EVENT, TASK, NOTE }

enum class Priority { NORMAL, IMPORTANT, CRITICAL }

/** How loud a single reminder should be. */
enum class ReminderType { NORMAL, IMPORTANT, ALARM }

/** Strategy used to nag the user in the days before an event. */
enum class AdvanceMode { CHAIN, INTERVAL, RANDOM }

enum class RepeatMode { NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM_DAYS, WEEKDAYS }

/** Why a scheduled notification exists — drives wording, channel and cleanup. */
enum class NotificationKind { MAIN, CHAIN, INTERVAL, RANDOM, CHECKLIST, SNOOZE, REPEAT }

data class RepeatRule(
    val mode: RepeatMode = RepeatMode.NONE,
    val interval: Int = 1,
    val weekDays: Set<Int> = emptySet(),
    val until: Long? = null,
) {
    val isRepeating: Boolean get() = mode != RepeatMode.NONE
}

data class AdvanceConfig(
    val enabled: Boolean = false,
    val mode: AdvanceMode = AdvanceMode.CHAIN,
    val startDaysBefore: Int = 14,
    val intervalDays: Int = 3,
    val perWeek: Int = 3,
    val windowStartMinutes: Int = 9 * 60,
    val windowEndMinutes: Int = 21 * 60,
    val workdaysOnly: Boolean = false,
    /** Chain steps (expressed as "minutes before") the user switched off. */
    val disabledChainSteps: Set<Int> = emptySet(),
    val checklistNudge: Boolean = false,
) {
    companion object {
        /** 14 days, 7 days, 3 days, 1 day, 3 hours, 30 minutes — expressed in minutes. */
        val CHAIN_STEPS = listOf(20160, 10080, 4320, 1440, 180, 30)
    }
}

data class Reminder(
    val id: Long = 0,
    val eventId: Long = 0,
    val minutesBefore: Int = 15,
    val type: ReminderType = ReminderType.NORMAL,
    val enabled: Boolean = true,
    val repeatUntilAck: Boolean = false,
    val repeatIntervalMinutes: Int = 5,
    val repeatMaxCount: Int = 3,
)

data class ChecklistItem(
    val id: Long = 0,
    val eventId: Long = 0,
    val text: String = "",
    val done: Boolean = false,
    val position: Int = 0,
)

data class Attachment(
    val id: Long = 0,
    val eventId: Long = 0,
    val uri: String = "",
    val name: String = "",
    val mime: String = "",
    val isLink: Boolean = false,
)

data class Category(
    val id: Long = 0,
    val name: String = "",
    val colorArgb: Int = 0xFF9E9E9E.toInt(),
    val builtInKey: String? = null,
)

data class PlanItem(
    val id: Long = 0,
    val type: ItemType = ItemType.EVENT,
    val title: String = "",
    val description: String = "",
    val notes: String = "",
    val startAt: Long? = null,
    val allDay: Boolean = false,
    val durationMinutes: Int = 60,
    val location: String = "",
    val url: String = "",
    val categoryId: Long? = null,
    val priority: Priority = Priority.NORMAL,
    val colorArgb: Int? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val repeat: RepeatRule = RepeatRule(),
    val advance: AdvanceConfig = AdvanceConfig(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    val endAt: Long? get() = startAt?.plus(durationMinutes.toLong() * 60_000L)
}

/** An event plus everything attached to it. */
data class PlanItemDetails(
    val item: PlanItem = PlanItem(),
    val reminders: List<Reminder> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
) {
    val checklistDone: Int get() = checklist.count { it.done }
    val checklistTotal: Int get() = checklist.size
}

data class ScheduledNotification(
    val id: Long = 0,
    val eventId: Long = 0,
    val reminderId: Long? = null,
    val triggerAt: Long = 0,
    val occurrenceStart: Long = 0,
    val kind: NotificationKind = NotificationKind.MAIN,
    val type: ReminderType = ReminderType.NORMAL,
    val repeatIndex: Int = 0,
    val fired: Boolean = false,
)
