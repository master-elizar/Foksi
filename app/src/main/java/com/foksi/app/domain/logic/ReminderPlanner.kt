package com.foksi.app.domain.logic

import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.AdvanceConfig
import com.foksi.app.domain.model.AdvanceMode
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Priority
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.domain.model.ScheduledNotification
import kotlin.math.max
import kotlin.random.Random

/**
 * Turns an event and its settings into a concrete list of notifications with absolute trigger
 * times. Pure logic: no Android, no database, so it can be reasoned about and unit tested.
 */
object ReminderPlanner {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /** How far ahead we materialise reminders. Beyond this the scheduler re-plans later. */
    const val PLAN_HORIZON_DAYS = 60

    fun plan(
        details: PlanItemDetails,
        settings: AppSettings,
        now: Long,
        horizonDays: Int = PLAN_HORIZON_DAYS,
    ): List<ScheduledNotification> {
        val item = details.item
        if (item.completed) return emptyList()
        if (item.type == ItemType.NOTE) return emptyList()
        val start = item.startAt ?: return emptyList()
        if (!settings.notificationsEnabled) return emptyList()

        val horizonEnd = now + horizonDays * DAY
        val occurrences = if (item.repeat.isRepeating) {
            RecurrenceEngine.occurrences(item, now - HOUR, horizonEnd, max = 4)
        } else {
            if (start > now - HOUR) listOf(start) else emptyList()
        }
        if (occurrences.isEmpty()) return emptyList()

        val result = mutableListOf<ScheduledNotification>()

        occurrences.forEachIndexed { index, occurrence ->
            result += mainReminders(details, occurrence, now)
            // Advance nagging only makes sense for the nearest occurrence of a repeating event.
            if (index == 0 && item.advance.enabled) {
                result += advanceReminders(item, occurrence, settings, now, horizonEnd)
            }
            if (index == 0 && item.advance.checklistNudge && details.checklist.isNotEmpty()) {
                val trigger = snapToWindow(occurrence - DAY, item.advance)
                if (trigger > now && trigger < occurrence) {
                    result += ScheduledNotification(
                        eventId = item.id,
                        triggerAt = trigger,
                        occurrenceStart = occurrence,
                        kind = NotificationKind.CHECKLIST,
                        type = advanceType(item.priority),
                    )
                }
            }
        }

        return dedupe(result, item, settings)
    }

    private fun mainReminders(
        details: PlanItemDetails,
        occurrence: Long,
        now: Long,
    ): List<ScheduledNotification> = details.reminders
        .filter { it.enabled }
        .mapNotNull { reminder ->
            val trigger = occurrence - reminder.minutesBefore * MINUTE
            if (trigger <= now) return@mapNotNull null
            ScheduledNotification(
                eventId = details.item.id,
                reminderId = reminder.id,
                triggerAt = trigger,
                occurrenceStart = occurrence,
                kind = NotificationKind.MAIN,
                type = reminder.type,
            )
        }

    private fun advanceReminders(
        item: PlanItem,
        occurrence: Long,
        settings: AppSettings,
        now: Long,
        horizonEnd: Long,
    ): List<ScheduledNotification> {
        val advance = item.advance
        val windowStart = occurrence - advance.startDaysBefore.coerceAtLeast(1) * DAY
        val from = max(now, windowStart)
        if (from >= occurrence) return emptyList()

        val type = advanceType(item.priority)
        val raw: List<Long> = when (advance.mode) {
            AdvanceMode.CHAIN -> AdvanceConfig.CHAIN_STEPS
                .filter { it !in advance.disabledChainSteps }
                .filter { it <= advance.startDaysBefore * 24 * 60 }
                .map { minutes ->
                    val trigger = occurrence - minutes * MINUTE
                    if (minutes >= 24 * 60) snapToWindow(trigger, advance) else trigger
                }

            AdvanceMode.INTERVAL -> {
                val step = advance.intervalDays.coerceAtLeast(1)
                generateSequence(advance.startDaysBefore.coerceAtLeast(1)) { it - step }
                    .takeWhile { it >= 1 }
                    .map { days -> snapToWindow(occurrence - days * DAY, advance) }
                    .toList()
            }

            AdvanceMode.RANDOM -> randomSlots(item, from, occurrence, advance)
        }

        return raw.asSequence()
            .filter { it > now && it < occurrence && it <= horizonEnd }
            .filter { !(advance.workdaysOnly && TimeUtils.isWeekend(it)) }
            .map { QuietHours.shiftOutOfQuiet(settings, it) }
            .filter { it < occurrence }
            .distinct()
            .sorted()
            .map { trigger ->
                ScheduledNotification(
                    eventId = item.id,
                    triggerAt = trigger,
                    occurrenceStart = occurrence,
                    kind = when (advance.mode) {
                        AdvanceMode.CHAIN -> NotificationKind.CHAIN
                        AdvanceMode.INTERVAL -> NotificationKind.INTERVAL
                        AdvanceMode.RANDOM -> NotificationKind.RANDOM
                    },
                    type = type,
                )
            }
            .toList()
    }

    /**
     * Spreads [AdvanceConfig.perWeek] reminders over each remaining week inside the allowed
     * hours. The generator is seeded with the event id and the week index, so re-planning the
     * same event produces the same slots instead of shuffling the user's day around.
     */
    private fun randomSlots(
        item: PlanItem,
        from: Long,
        occurrence: Long,
        advance: AdvanceConfig,
    ): List<Long> {
        val perWeek = advance.perWeek.coerceIn(0, 14)
        if (perWeek == 0) return emptyList()
        val windowStartMin = advance.windowStartMinutes.coerceIn(0, 1439)
        val windowEndMin = advance.windowEndMinutes.coerceIn(windowStartMin + 30, 1440)
        val slots = mutableListOf<Long>()
        var weekIndex = 0
        var weekStart = from
        while (weekStart < occurrence && weekIndex < 30) {
            val weekEnd = minOf(weekStart + 7 * DAY, occurrence)
            val days = ((weekEnd - weekStart) / DAY).toInt().coerceAtLeast(1)
            val random = Random(item.id * 1_000_003L + weekIndex * 7919L)
            val picks = perWeek.coerceAtMost(days)
            val chosenDays = mutableSetOf<Int>()
            var guard = 0
            while (chosenDays.size < picks && guard++ < 100) {
                chosenDays += random.nextInt(days)
            }
            chosenDays.sorted().forEach { dayOffset ->
                val dayBase = weekStart + dayOffset * DAY
                val minute = windowStartMin + random.nextInt(windowEndMin - windowStartMin)
                slots += TimeUtils.withTimeOfDay(dayBase, minute)
            }
            weekStart = weekEnd
            weekIndex++
        }
        return slots
    }

    private fun snapToWindow(trigger: Long, advance: AdvanceConfig): Long =
        TimeUtils.withTimeOfDay(trigger, advance.windowStartMinutes.coerceIn(0, 1439))

    private fun advanceType(priority: Priority): ReminderType =
        if (priority == Priority.CRITICAL) ReminderType.IMPORTANT else ReminderType.NORMAL

    /**
     * Keeps every explicit reminder but thins out the advance nagging so the user is never
     * spammed: no two advance notifications closer than the configured gap.
     */
    private fun dedupe(
        items: List<ScheduledNotification>,
        item: PlanItem,
        settings: AppSettings,
    ): List<ScheduledNotification> {
        val gapFactor = when (item.priority) {
            Priority.CRITICAL -> 0.5
            Priority.IMPORTANT -> 0.75
            Priority.NORMAL -> 1.0
        }
        val minGap = (settings.minGapHours.coerceAtLeast(1) * HOUR * gapFactor).toLong()
        val sorted = items.sortedBy { it.triggerAt }
        val mainTimes = sorted.filter { it.kind == NotificationKind.MAIN }.map { it.triggerAt }
        val kept = mutableListOf<ScheduledNotification>()
        // Null rather than Long.MIN_VALUE: subtracting the latter overflows and would silently
        // swallow every advance reminder.
        var lastSoft: Long? = null
        for (candidate in sorted) {
            if (candidate.kind == NotificationKind.MAIN) {
                kept += candidate
                continue
            }
            val previous = lastSoft
            if (previous != null && candidate.triggerAt - previous < minGap) continue
            // Never nag right before a real reminder fires anyway.
            if (mainTimes.any { kotlin.math.abs(it - candidate.triggerAt) < minGap }) continue
            kept += candidate
            lastSoft = candidate.triggerAt
        }
        return kept.sortedBy { it.triggerAt }
    }
}
