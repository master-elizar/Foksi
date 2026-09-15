package com.foksi.app.domain.logic

import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.RepeatMode
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Expands a repeat rule into concrete occurrences. Everything is computed with local date/time
 * arithmetic, so an event at 10:00 stays at 10:00 across daylight-saving changes.
 */
object RecurrenceEngine {

    private const val SAFETY_CAP = 2000

    fun occurrences(item: PlanItem, rangeStart: Long, rangeEnd: Long, max: Int = 200): List<Long> {
        val base = item.startAt ?: return emptyList()
        val rule = item.repeat
        if (!rule.isRepeating) {
            return if (base in rangeStart..rangeEnd) listOf(base) else emptyList()
        }
        val until = rule.until
        val hardEnd = if (until != null) minOf(rangeEnd, TimeUtils.endOfDay(until)) else rangeEnd
        if (hardEnd < rangeStart) return emptyList()

        val baseDateTime = TimeUtils.toLocalDateTime(base)
        val interval = rule.interval.coerceAtLeast(1)
        val result = mutableListOf<Long>()

        when (rule.mode) {
            RepeatMode.DAILY, RepeatMode.CUSTOM_DAYS -> {
                var current = fastForwardDays(baseDateTime, rangeStart, interval)
                var guard = 0
                while (guard++ < SAFETY_CAP && result.size < max) {
                    val millis = TimeUtils.toMillis(current)
                    if (millis > hardEnd) break
                    if (millis >= rangeStart) result += millis
                    current = current.plusDays(interval.toLong())
                }
            }

            RepeatMode.WEEKLY -> {
                if (rule.weekDays.isEmpty()) {
                    var current = fastForwardDays(baseDateTime, rangeStart, interval * 7)
                    var guard = 0
                    while (guard++ < SAFETY_CAP && result.size < max) {
                        val millis = TimeUtils.toMillis(current)
                        if (millis > hardEnd) break
                        if (millis >= rangeStart) result += millis
                        current = current.plusWeeks(interval.toLong())
                    }
                } else {
                    var day = maxOf(baseDateTime, TimeUtils.toLocalDateTime(rangeStart))
                        .toLocalDate()
                    val baseDate = baseDateTime.toLocalDate()
                    var guard = 0
                    while (guard++ < SAFETY_CAP && result.size < max) {
                        val candidate = LocalDateTime.of(day, baseDateTime.toLocalTime())
                        val millis = TimeUtils.toMillis(candidate)
                        if (millis > hardEnd) break
                        val weeksApart = ChronoUnit.WEEKS.between(
                            baseDate.with(DayOfWeek.MONDAY), day.with(DayOfWeek.MONDAY)
                        )
                        val matchesWeek = weeksApart >= 0 && weeksApart % interval == 0L
                        if (matchesWeek && rule.weekDays.contains(day.dayOfWeek.value) &&
                            millis >= rangeStart && millis >= base
                        ) {
                            result += millis
                        }
                        day = day.plusDays(1)
                    }
                }
            }

            RepeatMode.WEEKDAYS -> {
                var day = maxOf(baseDateTime, TimeUtils.toLocalDateTime(rangeStart)).toLocalDate()
                var guard = 0
                while (guard++ < SAFETY_CAP && result.size < max) {
                    val candidate = LocalDateTime.of(day, baseDateTime.toLocalTime())
                    val millis = TimeUtils.toMillis(candidate)
                    if (millis > hardEnd) break
                    val weekday = day.dayOfWeek != DayOfWeek.SATURDAY && day.dayOfWeek != DayOfWeek.SUNDAY
                    if (weekday && millis >= rangeStart && millis >= base) result += millis
                    day = day.plusDays(1)
                }
            }

            RepeatMode.MONTHLY -> {
                val monthsApart = ChronoUnit.MONTHS.between(
                    baseDateTime.toLocalDate().withDayOfMonth(1),
                    TimeUtils.toLocalDate(rangeStart).withDayOfMonth(1)
                )
                var step = (monthsApart / interval) * interval
                if (step < 0) step = 0
                var guard = 0
                while (guard++ < SAFETY_CAP && result.size < max) {
                    val candidate = baseDateTime.plusMonths(step)
                    val millis = TimeUtils.toMillis(candidate)
                    if (millis > hardEnd) break
                    if (millis >= rangeStart && millis >= base) result += millis
                    step += interval.toLong()
                }
            }

            RepeatMode.YEARLY -> {
                val yearsApart = (TimeUtils.toLocalDate(rangeStart).year - baseDateTime.year).toLong()
                var step = (yearsApart / interval) * interval
                if (step < 0) step = 0
                var guard = 0
                while (guard++ < SAFETY_CAP && result.size < max) {
                    val candidate = baseDateTime.plusYears(step)
                    val millis = TimeUtils.toMillis(candidate)
                    if (millis > hardEnd) break
                    if (millis >= rangeStart && millis >= base) result += millis
                    step += interval.toLong()
                }
            }

            RepeatMode.NONE -> Unit
        }
        return result
    }

    /** Next [count] occurrences at or after [from]. */
    fun nextOccurrences(item: PlanItem, from: Long, count: Int = 1): List<Long> {
        val base = item.startAt ?: return emptyList()
        if (!item.repeat.isRepeating) return if (base >= from) listOf(base) else emptyList()
        // Two years is enough for any sane rule; yearly repeats need the wider window.
        val end = from + 366L * 3 * 24 * 60 * 60 * 1000L
        return occurrences(item, from, end, count)
    }

    private fun fastForwardDays(base: LocalDateTime, rangeStart: Long, stepDays: Int): LocalDateTime {
        val startDateTime = TimeUtils.toLocalDateTime(rangeStart)
        if (!base.isBefore(startDateTime)) return base
        val daysApart = ChronoUnit.DAYS.between(base.toLocalDate(), startDateTime.toLocalDate())
        val steps = daysApart / stepDays
        return base.plusDays(steps * stepDays)
    }
}
