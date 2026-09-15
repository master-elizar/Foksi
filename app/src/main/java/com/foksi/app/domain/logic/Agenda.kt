package com.foksi.app.domain.logic

import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Priority
import java.time.DayOfWeek
import java.time.LocalDate

enum class AgendaGroup {
    OVERDUE, TODAY, TOMORROW, THIS_WEEK, NEXT_WEEK, THIS_MONTH, LATER, NO_DATE, COMPLETED
}

data class AgendaSection(val group: AgendaGroup, val items: List<PlanItemDetails>)

enum class PlanFilter { ALL, TODAY, TOMORROW, WEEK, IMPORTANT, OVERDUE, COMPLETED, WITH_NOTES }

object Agenda {

    /** For a repeating item the interesting date is its next occurrence, not the original one. */
    fun displayStart(details: PlanItemDetails, now: Long): Long? {
        val item = details.item
        val start = item.startAt ?: return null
        if (!item.repeat.isRepeating) return start
        val from = minOf(now, TimeUtils.startOfDay(now))
        return RecurrenceEngine.nextOccurrences(item, from, 1).firstOrNull() ?: start
    }

    fun groupOf(start: Long?, completed: Boolean, now: Long): AgendaGroup {
        if (completed) return AgendaGroup.COMPLETED
        if (start == null) return AgendaGroup.NO_DATE
        val today = TimeUtils.toLocalDate(now)
        val date = TimeUtils.toLocalDate(start)
        return when {
            start < now && date.isBefore(today) -> AgendaGroup.OVERDUE
            date == today -> AgendaGroup.TODAY
            date == today.plusDays(1) -> AgendaGroup.TOMORROW
            date.isBefore(endOfWeek(today).plusDays(1)) -> AgendaGroup.THIS_WEEK
            date.isBefore(endOfWeek(today).plusDays(8)) -> AgendaGroup.NEXT_WEEK
            date.year == today.year && date.month == today.month -> AgendaGroup.THIS_MONTH
            else -> AgendaGroup.LATER
        }
    }

    private fun endOfWeek(today: LocalDate): LocalDate {
        var date = today
        while (date.dayOfWeek != DayOfWeek.SUNDAY) date = date.plusDays(1)
        return date
    }

    private val ORDER = listOf(
        AgendaGroup.OVERDUE,
        AgendaGroup.TODAY,
        AgendaGroup.TOMORROW,
        AgendaGroup.THIS_WEEK,
        AgendaGroup.NEXT_WEEK,
        AgendaGroup.THIS_MONTH,
        AgendaGroup.LATER,
        AgendaGroup.NO_DATE,
        AgendaGroup.COMPLETED,
    )

    fun sections(items: List<PlanItemDetails>, now: Long): List<AgendaSection> {
        val enriched = items.map { it to displayStart(it, now) }
        val grouped = enriched.groupBy { (details, start) ->
            groupOf(start, details.item.completed, now)
        }
        return ORDER.mapNotNull { group ->
            val entries = grouped[group] ?: return@mapNotNull null
            val sorted = entries.sortedWith(
                compareBy({ it.second ?: Long.MAX_VALUE }, { it.first.item.title })
            ).map { it.first }
            AgendaSection(group, sorted)
        }
    }

    fun filter(
        items: List<PlanItemDetails>,
        filter: PlanFilter,
        now: Long,
        categoryId: Long? = null,
    ): List<PlanItemDetails> {
        val today = TimeUtils.toLocalDate(now)
        return items.filter { details ->
            val item = details.item
            if (categoryId != null && item.categoryId != categoryId) return@filter false
            val start = displayStart(details, now)
            when (filter) {
                PlanFilter.ALL -> !item.completed
                PlanFilter.TODAY -> !item.completed && start != null && TimeUtils.toLocalDate(start) == today
                PlanFilter.TOMORROW -> !item.completed && start != null &&
                    TimeUtils.toLocalDate(start) == today.plusDays(1)

                PlanFilter.WEEK -> !item.completed && start != null &&
                    !TimeUtils.toLocalDate(start).isBefore(today) &&
                    TimeUtils.toLocalDate(start).isBefore(today.plusDays(8))

                PlanFilter.IMPORTANT -> !item.completed && item.priority != Priority.NORMAL
                PlanFilter.OVERDUE -> !item.completed && start != null && start < now &&
                    TimeUtils.toLocalDate(start).isBefore(today)

                PlanFilter.COMPLETED -> item.completed
                PlanFilter.WITH_NOTES -> item.notes.isNotBlank() || details.checklist.isNotEmpty()
            }
        }
    }

    /** The one card the home screen leads with. */
    fun nextUp(items: List<PlanItemDetails>, now: Long): PlanItemDetails? = items
        .asSequence()
        .filter { !it.item.completed && it.item.type != ItemType.NOTE }
        .mapNotNull { details -> displayStart(details, now)?.let { details to it } }
        .filter { it.second >= now }
        .sortedBy { it.second }
        .firstOrNull()
        ?.first
}
