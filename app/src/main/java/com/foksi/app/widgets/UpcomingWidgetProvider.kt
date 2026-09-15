package com.foksi.app.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.notifications.Extras

/** 4×2: the upcoming list, grouped by day. */
class UpcomingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, manager, it) }
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
        val height = manager.getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        val limit = (height / 56).coerceIn(2, 12)
        val hasRows = runCatching { WidgetData.load(context, limit).rows.isNotEmpty() }.getOrDefault(false)

        val views = RemoteViews(context.packageName, R.layout.widget_upcoming).apply {
            setTextViewText(R.id.widget_header, TimeUtils.formatWeekdayDate(context, TimeUtils.now()))
            setOnClickPendingIntent(
                R.id.widget_header,
                WidgetIntents.activity(context, Extras.ACTION_OPEN_CALENDAR, REQ_CALENDAR)
            )
            setOnClickPendingIntent(
                R.id.widget_add,
                WidgetIntents.activity(context, Extras.ACTION_QUICK_ADD, REQ_ADD)
            )
            setRemoteAdapter(R.id.widget_list, listServiceIntent(context, appWidgetId, limit))
            setPendingIntentTemplate(R.id.widget_list, WidgetIntents.listTemplate(context, REQ_TEMPLATE))
            setEmptyView(R.id.widget_list, R.id.widget_empty)
            setViewVisibility(R.id.widget_list, if (hasRows) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.widget_empty, if (hasRows) View.GONE else View.VISIBLE)
        }
        runCatching { manager.updateAppWidget(appWidgetId, views) }
        runCatching { manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list) }
    }

    private companion object {
        const val REQ_CALENDAR = 8201
        const val REQ_ADD = 8202
        const val REQ_TEMPLATE = 8203
    }
}
