package com.foksi.app.widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.foksi.app.MainActivity
import com.foksi.app.notifications.Extras

internal object WidgetIntents {

    private fun flags() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun activity(context: Context, action: String, requestCode: Int, eventId: Long? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            if (eventId != null) putExtra(Extras.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags())
    }

    /** Template used by the collection widgets; each row supplies its own event id. */
    fun listTemplate(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Extras.ACTION_OPEN_EVENT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
