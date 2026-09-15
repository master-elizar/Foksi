package com.foksi.app

import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.logic.Birthdays
import com.foksi.app.domain.logic.ReminderPlanner
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class BirthdayTest {

    private val today: LocalDate = LocalDate.now()
    private val now: Long = TimeUtils.toMillis(today, LocalTime.of(12, 0))

    private fun birthday(date: LocalDate, yearKnown: Boolean = true) = Birthdays.asBirthday(
        PlanItem(
            id = 1,
            type = ItemType.BIRTHDAY,
            title = "Иван",
            startAt = TimeUtils.toMillis(date, LocalTime.of(Birthdays.DEFAULT_HOUR, 0)),
            birthYearKnown = yearKnown,
        )
    )

    @Test
    fun `a birthday later this month is found this year`() {
        val date = today.plusDays(10).withYear(1990)
        val item = birthday(date)

        val next = Birthdays.nextOccurrence(item, now)

        assertTrue(next != null)
        assertEquals(today.plusDays(10), TimeUtils.toLocalDate(next!!))
    }

    @Test
    fun `age counts from the birth year to the next celebration`() {
        val date = today.plusDays(5).withYear(1990)
        val item = birthday(date)

        val age = Birthdays.ageTurning(item, now)

        assertEquals(TimeUtils.toLocalDate(Birthdays.nextOccurrence(item, now)!!).year - 1990, age)
    }

    @Test
    fun `an unknown birth year hides the age`() {
        val item = birthday(today.plusDays(3).withYear(1990), yearKnown = false)

        assertNull(Birthdays.ageTurning(item, now))
    }

    @Test
    fun `today's birthday reports zero days left`() {
        val item = birthday(today.withYear(1990))

        assertEquals(0L, Birthdays.daysUntil(item, now))
    }

    @Test
    fun `birthday reminders are scheduled ahead of the celebration`() {
        val date = today.plusDays(10).withYear(1990)
        val details = PlanItemDetails(
            item = birthday(date),
            reminders = Birthdays.DEFAULT_REMINDER_OFFSETS.mapIndexed { index, offset ->
                Reminder(id = index + 1L, eventId = 1, minutesBefore = offset)
            },
        )

        val planned = ReminderPlanner.plan(details, AppSettings(quietHoursEnabled = false), now)
        val main = planned.filter { it.kind == NotificationKind.MAIN }

        // A week ahead has already passed for a birthday ten days out, so two of three remain.
        assertTrue("expected at least two reminders, got ${main.size}", main.size >= 2)
        assertTrue(main.all { it.triggerAt > now })
    }
}
