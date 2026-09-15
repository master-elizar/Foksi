package com.foksi.app.domain.logic

import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.RepeatMode
import com.foksi.app.domain.model.RepeatRule
import java.time.LocalDate

/**
 * Birthdays are ordinary yearly-repeating items, so the whole reminder engine works on them
 * unchanged. This object only adds the two things a birthday needs and an event does not:
 * the next celebration date and the age the person is turning.
 */
object Birthdays {

    /** Time of day a birthday reminder fires when the user has not chosen anything else. */
    const val DEFAULT_HOUR = 9

    val DEFAULT_REMINDER_OFFSETS = listOf(7 * 24 * 60, 24 * 60, 0)

    fun asBirthday(item: PlanItem): PlanItem = item.copy(
        type = ItemType.BIRTHDAY,
        allDay = true,
        durationMinutes = 60,
        repeat = RepeatRule(mode = RepeatMode.YEARLY, interval = 1),
    )

    /** The next time this birthday comes around, counted from [from]. */
    fun nextOccurrence(item: PlanItem, from: Long = TimeUtils.now()): Long? {
        val birth = item.startAt ?: return null
        val dayStart = TimeUtils.startOfDay(from)
        return RecurrenceEngine.nextOccurrences(item, dayStart, 1).firstOrNull() ?: birth
    }

    fun birthDate(item: PlanItem): LocalDate? = item.startAt?.let { TimeUtils.toLocalDate(it) }

    /** Age the person turns on their next birthday, or null when the birth year is unknown. */
    fun ageTurning(item: PlanItem, from: Long = TimeUtils.now()): Int? {
        if (!item.birthYearKnown) return null
        val birth = birthDate(item) ?: return null
        val next = nextOccurrence(item, from) ?: return null
        val age = TimeUtils.toLocalDate(next).year - birth.year
        return if (age in 0..150) age else null
    }

    /** How many days are left until the next celebration; 0 means it is today. */
    fun daysUntil(item: PlanItem, from: Long = TimeUtils.now()): Long {
        val next = nextOccurrence(item, from) ?: return Long.MAX_VALUE
        return TimeUtils.daysBetween(from, next).coerceAtLeast(0)
    }

    fun sort(items: List<PlanItemDetails>, from: Long = TimeUtils.now()): List<PlanItemDetails> =
        items.sortedBy { daysUntil(it.item, from) }
}
