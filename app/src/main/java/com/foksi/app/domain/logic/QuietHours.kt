package com.foksi.app.domain.logic

import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.ReminderType

/** Quiet hours may wrap around midnight (22:00 → 08:00), which is the common case. */
object QuietHours {

    fun isQuietMinute(settings: AppSettings, minuteOfDay: Int): Boolean {
        if (!settings.quietHoursEnabled) return false
        val from = settings.quietFromMinutes
        val to = settings.quietToMinutes
        if (from == to) return false
        return if (from < to) minuteOfDay in from until to else minuteOfDay >= from || minuteOfDay < to
    }

    fun isQuiet(settings: AppSettings, millis: Long): Boolean =
        isQuietMinute(settings, TimeUtils.minutesOfDay(millis))

    /** True when a notification of this type must stay silent right now. */
    fun blocks(settings: AppSettings, type: ReminderType, millis: Long): Boolean {
        if (!isQuiet(settings, millis)) return false
        return when (type) {
            ReminderType.ALARM -> !settings.quietAllowAlarms
            ReminderType.IMPORTANT -> !settings.quietAllowImportant
            ReminderType.NORMAL -> true
        }
    }

    /** Moves a trigger forward to the first allowed minute after the quiet window. */
    fun shiftOutOfQuiet(settings: AppSettings, millis: Long): Long {
        if (!isQuiet(settings, millis)) return millis
        val minuteOfDay = TimeUtils.minutesOfDay(millis)
        val to = settings.quietToMinutes
        val sameDay = minuteOfDay < to
        val base = if (sameDay) millis else millis + 24L * 60 * 60 * 1000
        return TimeUtils.withTimeOfDay(base, to)
    }
}
