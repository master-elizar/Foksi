package com.foksi.app

import com.foksi.app.domain.logic.RecurrenceEngine
import com.foksi.app.domain.logic.ReminderPlanner
import com.foksi.app.domain.model.AdvanceConfig
import com.foksi.app.domain.model.AdvanceMode
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Reminder
import com.foksi.app.domain.model.RepeatMode
import com.foksi.app.domain.model.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

class ReminderPlannerTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `explicit reminders are planned before the event`() {
        val start = now + 14 * DAY
        val details = PlanItemDetails(
            item = PlanItem(id = 1, title = "Interview", startAt = start),
            reminders = listOf(
                Reminder(id = 1, eventId = 1, minutesBefore = 7 * 24 * 60),
                Reminder(id = 2, eventId = 1, minutesBefore = 60),
                Reminder(id = 3, eventId = 1, minutesBefore = 15),
            ),
        )

        val planned = ReminderPlanner.plan(details, AppSettings(quietHoursEnabled = false), now)

        assertEquals(3, planned.count { it.kind == NotificationKind.MAIN })
        assertTrue(planned.all { it.triggerAt in (now + 1)..start })
    }

    @Test
    fun `past reminders are skipped`() {
        val start = now + 30 * MINUTE
        val details = PlanItemDetails(
            item = PlanItem(id = 2, title = "Call", startAt = start),
            reminders = listOf(
                Reminder(id = 1, eventId = 2, minutesBefore = 24 * 60),
                Reminder(id = 2, eventId = 2, minutesBefore = 15),
            ),
        )

        val planned = ReminderPlanner.plan(details, AppSettings(quietHoursEnabled = false), now)

        assertEquals(1, planned.size)
    }

    @Test
    fun `preparation chain produces several nudges spread over time`() {
        val start = now + 20 * DAY
        val details = PlanItemDetails(
            item = PlanItem(
                id = 3,
                title = "Interview",
                startAt = start,
                advance = AdvanceConfig(enabled = true, mode = AdvanceMode.CHAIN, startDaysBefore = 14),
            ),
        )

        val planned = ReminderPlanner.plan(details, AppSettings(quietHoursEnabled = false), now)
        val chain = planned.filter { it.kind == NotificationKind.CHAIN }

        assertTrue("expected a chain of nudges, got ${chain.size}", chain.size >= 4)
        assertTrue(chain.zipWithNext().all { (a, b) -> a.triggerAt < b.triggerAt })
    }

    @Test
    fun `random reminders never violate the minimum gap`() {
        val start = now + 30 * DAY
        val details = PlanItemDetails(
            item = PlanItem(
                id = 4,
                title = "Exam",
                startAt = start,
                advance = AdvanceConfig(
                    enabled = true,
                    mode = AdvanceMode.RANDOM,
                    startDaysBefore = 30,
                    perWeek = 3,
                ),
            ),
        )
        val settings = AppSettings(quietHoursEnabled = false, minGapHours = 12)

        val planned = ReminderPlanner.plan(details, settings, now)
        val random = planned.filter { it.kind == NotificationKind.RANDOM }

        assertTrue(random.isNotEmpty())
        random.zipWithNext().forEach { (a, b) ->
            assertTrue("gap too small", b.triggerAt - a.triggerAt >= 12 * HOUR)
        }
    }

    @Test
    fun `completed events are never scheduled`() {
        val details = PlanItemDetails(
            item = PlanItem(id = 5, title = "Done", startAt = now + DAY, completed = true),
            reminders = listOf(Reminder(id = 1, eventId = 5, minutesBefore = 30)),
        )

        assertTrue(ReminderPlanner.plan(details, AppSettings(), now).isEmpty())
    }

    @Test
    fun `weekly recurrence produces weekly occurrences`() {
        val item = PlanItem(
            id = 6,
            title = "Team sync",
            startAt = now,
            repeat = RepeatRule(mode = RepeatMode.WEEKLY, interval = 1),
        )

        val occurrences = RecurrenceEngine.occurrences(item, now, now + 28 * DAY)

        assertEquals(5, occurrences.size)
        occurrences.zipWithNext().forEach { (a, b) -> assertEquals(7 * DAY, b - a) }
    }

    @Test
    fun `daily recurrence respects the end date`() {
        val item = PlanItem(
            id = 7,
            title = "Pills",
            startAt = now,
            repeat = RepeatRule(mode = RepeatMode.DAILY, interval = 1, until = now + 3 * DAY),
        )

        val occurrences = RecurrenceEngine.occurrences(item, now, now + 30 * DAY)

        assertTrue(occurrences.size in 4..5)
        assertTrue(occurrences.all { it <= now + 4 * DAY })
    }
}
