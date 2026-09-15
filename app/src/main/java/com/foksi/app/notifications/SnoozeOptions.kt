package com.foksi.app.notifications

import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import java.time.LocalTime

/** The snooze choices offered on notifications and on the alarm screen. */
enum class SnoozeOption(val labelRes: Int) {
    MIN_5(R.string.snooze_5m),
    MIN_10(R.string.snooze_10m),
    MIN_30(R.string.snooze_30m),
    HOUR_1(R.string.snooze_1h),
    HOUR_2(R.string.snooze_2h),
    TONIGHT(R.string.snooze_tonight),
    TOMORROW(R.string.snooze_tomorrow),
    PICK(R.string.snooze_pick);

    /** Absolute trigger time for this option, or null when the user has to pick one. */
    fun triggerAt(now: Long = TimeUtils.now()): Long? = when (this) {
        MIN_5 -> now + 5 * 60_000L
        MIN_10 -> now + 10 * 60_000L
        MIN_30 -> now + 30 * 60_000L
        HOUR_1 -> now + 60 * 60_000L
        HOUR_2 -> now + 120 * 60_000L
        TONIGHT -> {
            val tonight = TimeUtils.withTimeOfDay(now, 19 * 60)
            if (tonight > now) tonight else TimeUtils.withTimeOfDay(now + 86_400_000L, 19 * 60)
        }
        TOMORROW -> TimeUtils.toMillis(
            TimeUtils.toLocalDate(now).plusDays(1), LocalTime.of(9, 0)
        )
        PICK -> null
    }
}
