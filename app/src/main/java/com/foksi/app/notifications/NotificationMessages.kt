package com.foksi.app.notifications

import android.content.Context
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.PlanItem

/**
 * Wording lives here so that a notification always speaks the language the phone is set to,
 * even if it was scheduled months earlier in another language.
 */
object NotificationMessages {

    fun title(
        context: Context,
        kind: NotificationKind,
        item: PlanItem,
        occurrenceStart: Long,
        now: Long = TimeUtils.now(),
    ): String {
        if (kind == NotificationKind.MAIN || kind == NotificationKind.SNOOZE || kind == NotificationKind.REPEAT) {
            val sameDay = TimeUtils.isSameDay(now, occurrenceStart)
            return if (item.isBirthday && sameDay) {
                context.getString(R.string.msg_birthday_today, item.title)
            } else {
                item.title
            }
        }
        val days = TimeUtils.daysBetween(now, occurrenceStart).toInt()
        if (item.isBirthday && days <= 0) {
            return context.getString(R.string.msg_birthday_today, item.title)
        }
        return when {
            days >= 14 -> context.getString(R.string.msg_two_weeks, item.title)
            days == 7 -> context.getString(R.string.msg_week, item.title)
            days >= 2 -> context.resources.getQuantityString(
                R.plurals.msg_advance_days, days, item.title, days
            )
            days == 1 -> context.getString(R.string.msg_tomorrow, item.title)
            else -> {
                val minutes = ((occurrenceStart - now) / 60_000).toInt().coerceAtLeast(0)
                if (minutes >= 60) {
                    context.getString(R.string.msg_in_hours, item.title, minutes / 60)
                } else {
                    context.getString(R.string.msg_in_minutes, item.title, minutes.coerceAtLeast(1))
                }
            }
        }
    }

    fun text(
        context: Context,
        kind: NotificationKind,
        item: PlanItem,
        occurrenceStart: Long,
        use24h: Boolean,
        openChecklistItems: Int,
        now: Long = TimeUtils.now(),
    ): String {
        val when24 = TimeUtils.formatDateTime(context, occurrenceStart, use24h)
        val countdown = TimeUtils.formatCountdown(context, occurrenceStart, now)
        val base = when (kind) {
            NotificationKind.MAIN, NotificationKind.SNOOZE, NotificationKind.REPEAT ->
                "$countdown · $when24"

            NotificationKind.CHECKLIST ->
                context.getString(R.string.notif_checklist_left, openChecklistItems)

            else -> when24
        }
        val place = item.location.takeIf { it.isNotBlank() }
        return if (place != null) "$base · $place" else base
    }
}
