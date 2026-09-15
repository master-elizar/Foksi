package com.foksi.app.core

import android.content.Context
import com.foksi.app.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/**
 * Everything time related is kept in one place so the app always agrees with itself about
 * what "today" means, even when the device time zone changes while the app is alive.
 */
object TimeUtils {

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun now(): Long = System.currentTimeMillis()

    fun toLocalDateTime(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone()).toLocalDateTime()

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()

    fun toMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone()).toInstant().toEpochMilli()

    fun toMillis(date: LocalDate, time: LocalTime): Long = toMillis(LocalDateTime.of(date, time))

    fun startOfDay(millis: Long): Long = toMillis(toLocalDate(millis), LocalTime.MIN)

    fun endOfDay(millis: Long): Long = toMillis(toLocalDate(millis).plusDays(1), LocalTime.MIN) - 1

    fun startOfDay(date: LocalDate): Long = toMillis(date, LocalTime.MIN)

    fun endOfDay(date: LocalDate): Long = toMillis(date.plusDays(1), LocalTime.MIN) - 1

    /** Calendar days between two instants, counting day boundaries rather than 24h blocks. */
    fun daysBetween(from: Long, to: Long): Long =
        ChronoUnit.DAYS.between(toLocalDate(from), toLocalDate(to))

    fun minutesOfDay(millis: Long): Int = toLocalDateTime(millis).toLocalTime().toSecondOfDay() / 60

    fun withTimeOfDay(millis: Long, minutesOfDay: Int): Long =
        toMillis(toLocalDate(millis), LocalTime.ofSecondOfDay(minutesOfDay.toLong() * 60L))

    fun isSameDay(a: Long, b: Long): Boolean = toLocalDate(a) == toLocalDate(b)

    fun isToday(millis: Long): Boolean = toLocalDate(millis) == LocalDate.now(zone())

    fun isTomorrow(millis: Long): Boolean = toLocalDate(millis) == LocalDate.now(zone()).plusDays(1)

    fun isWeekend(millis: Long): Boolean {
        val day = toLocalDate(millis).dayOfWeek
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
    }

    private fun locale(context: Context): Locale =
        context.resources.configuration.locales[0] ?: Locale.getDefault()

    fun formatTime(context: Context, millis: Long, use24h: Boolean): String {
        val pattern = if (use24h) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, locale(context)).format(toLocalDateTime(millis))
    }

    fun formatDate(context: Context, millis: Long): String {
        val date = toLocalDate(millis)
        val pattern = if (date.year == LocalDate.now(zone()).year) "d MMMM" else "d MMMM yyyy"
        return DateTimeFormatter.ofPattern(pattern, locale(context)).format(date)
    }

    /** Always includes the year — used for birth dates, where the year carries meaning. */
    fun formatFullDate(context: Context, millis: Long): String =
        DateTimeFormatter.ofPattern("d MMMM yyyy", locale(context)).format(toLocalDate(millis))

    fun formatShortDate(context: Context, millis: Long): String =
        DateTimeFormatter.ofPattern("d MMM", locale(context)).format(toLocalDate(millis))

    fun formatWeekdayDate(context: Context, millis: Long): String {
        val date = toLocalDate(millis)
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale(context))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale(context)) else it.toString() }
        return "$weekday, ${formatDate(context, millis)}"
    }

    fun formatDateTime(context: Context, millis: Long, use24h: Boolean): String =
        "${formatDate(context, millis)} · ${formatTime(context, millis, use24h)}"

    fun formatMonthYear(context: Context, date: LocalDate): String =
        DateTimeFormatter.ofPattern("LLLL yyyy", locale(context)).format(date)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale(context)) else it.toString() }

    fun formatDuration(context: Context, minutes: Int): String = when {
        minutes < 60 -> context.getString(R.string.duration_minutes, minutes)
        minutes % 60 == 0 -> context.getString(R.string.duration_hours, minutes / 60)
        else -> context.getString(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    }

    fun formatOffset(context: Context, minutes: Int): String = when {
        minutes <= 0 -> context.getString(R.string.reminder_at_time)
        minutes % (60 * 24 * 7) == 0 -> context.resources.getQuantityString(
            R.plurals.countdown_weeks, minutes / (60 * 24 * 7), minutes / (60 * 24 * 7)
        ).substringAfter(' ')
        minutes % (60 * 24) == 0 -> context.resources.getQuantityString(
            R.plurals.days_short, minutes / (60 * 24), minutes / (60 * 24)
        )
        minutes % 60 == 0 -> context.getString(R.string.duration_hours, minutes / 60)
        else -> context.getString(R.string.duration_minutes, minutes)
    }

    /** Human countdown such as "in 14 days" / "через 14 дней". */
    fun formatCountdown(context: Context, target: Long, from: Long = now()): String {
        val diff = target - from
        if (diff <= 0) return context.getString(R.string.countdown_past)
        val minutes = diff / 60_000
        if (minutes < 1) return context.getString(R.string.countdown_now)
        if (minutes < 60) {
            val m = minutes.toInt()
            return context.resources.getQuantityString(R.plurals.countdown_minutes, m, m)
        }
        val days = daysBetween(from, target)
        if (days < 1) {
            val h = (minutes / 60).toInt()
            return context.resources.getQuantityString(R.plurals.countdown_hours, h, h)
        }
        if (days < 14) {
            val d = days.toInt()
            return context.resources.getQuantityString(R.plurals.countdown_days, d, d)
        }
        val weeks = (days / 7).toInt()
        return context.resources.getQuantityString(R.plurals.countdown_weeks, weeks, weeks)
    }

    /** Compact countdown for widgets: "14 д" style is avoided, we reuse the localized plural. */
    fun formatCountdownShort(context: Context, target: Long, from: Long = now()): String {
        val diff = target - from
        if (diff <= 0) return context.getString(R.string.countdown_past)
        val days = daysBetween(from, target)
        return if (days >= 1) {
            context.resources.getQuantityString(R.plurals.days_short, days.toInt(), days.toInt())
        } else {
            val minutes = (diff / 60_000).toInt()
            if (minutes < 60) context.getString(R.string.duration_minutes, minutes.coerceAtLeast(1))
            else context.getString(R.string.duration_hours, minutes / 60)
        }
    }

    fun formatClock(minutesOfDay: Int, use24h: Boolean): String {
        val m = ((minutesOfDay % 1440) + 1440) % 1440
        val time = LocalTime.ofSecondOfDay(m.toLong() * 60L)
        val pattern = if (use24h) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(time)
    }

    fun firstDayOfWeek(setting: Int): DayOfWeek = if (setting == 7) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

    fun clampPositive(value: Long): Long = if (value < 0) 0 else value

    fun absDiffMinutes(a: Long, b: Long): Long = abs(a - b) / 60_000
}
