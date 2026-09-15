package com.foksi.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.foksi.app.R
import com.foksi.app.notifications.Extras

/** 2×2: the single thing that is coming up next, with its countdown. */
class NextEventWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> render(context, manager, id) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        render(context, manager, appWidgetId)
    }

    private fun render(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val snapshot = runCatching { WidgetData.load(context, 1) }.getOrNull()
        val next = snapshot?.next
        val height = manager.getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)

        val views = RemoteViews(context.packageName, R.layout.widget_next).apply {
            setTextViewText(R.id.widget_label, context.getString(R.string.home_next_title))
            if (next == null) {
                setTextViewText(R.id.widget_title, context.getString(R.string.widget_empty))
                setTextViewText(R.id.widget_datetime, "")
                setViewVisibility(R.id.widget_datetime, View.GONE)
                setViewVisibility(R.id.widget_countdown, View.GONE)
                setOnClickPendingIntent(
                    R.id.widget_root,
                    WidgetIntents.activity(context, Extras.ACTION_QUICK_ADD, REQ_ADD)
                )
            } else {
                setTextViewText(R.id.widget_title, next.title)
                setTextViewText(R.id.widget_datetime, next.subtitle)
                setViewVisibility(R.id.widget_datetime, if (height < 90) View.GONE else View.VISIBLE)
                setTextViewText(R.id.widget_countdown, next.countdown)
                setViewVisibility(
                    R.id.widget_countdown,
                    if (next.countdown.isBlank() || height < 110) View.GONE else View.VISIBLE
                )
                setOnClickPendingIntent(
                    R.id.widget_root,
                    WidgetIntents.activity(context, Extras.ACTION_OPEN_EVENT, REQ_OPEN, next.eventId)
                )
            }
        }
        runCatching { manager.updateAppWidget(appWidgetId, views) }
    }

    private companion object {
        const val REQ_OPEN = 8101
        const val REQ_ADD = 8102

        @Suppress("unused")
        fun unusedFlags(): Int = PendingIntent.FLAG_IMMUTABLE
    }
}
