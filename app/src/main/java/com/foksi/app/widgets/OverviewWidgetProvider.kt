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

/** 4×4: hero countdown plus the rest of the list, tasks and notes included. */
class OverviewWidgetProvider : AppWidgetProvider() {

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
        val options = manager.getAppWidgetOptions(appWidgetId)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250)
        val limit = ((height - 120) / 56).coerceIn(2, 14)
        val snapshot = runCatching { WidgetData.load(context, limit) }.getOrNull()
        val next = snapshot?.next
        val hasRows = snapshot?.rows?.isNotEmpty() == true

        val views = RemoteViews(context.packageName, R.layout.widget_overview).apply {
            setTextViewText(R.id.widget_header, TimeUtils.formatWeekdayDate(context, TimeUtils.now()))
            setOnClickPendingIntent(
                R.id.widget_header,
                WidgetIntents.activity(context, Extras.ACTION_OPEN_CALENDAR, REQ_CALENDAR)
            )
            setOnClickPendingIntent(
                R.id.widget_add,
                WidgetIntents.activity(context, Extras.ACTION_QUICK_ADD, REQ_ADD)
            )

            if (next == null || height < 180) {
                setViewVisibility(R.id.widget_hero, View.GONE)
            } else {
                setViewVisibility(R.id.widget_hero, View.VISIBLE)
                setTextViewText(R.id.widget_hero_title, next.title)
                setTextViewText(R.id.widget_hero_datetime, next.subtitle)
                setTextViewText(R.id.widget_hero_countdown, next.countdown)
                setViewVisibility(
                    R.id.widget_hero_countdown,
                    if (next.countdown.isBlank()) View.GONE else View.VISIBLE
                )
                setOnClickPendingIntent(
                    R.id.widget_hero,
                    WidgetIntents.activity(context, Extras.ACTION_OPEN_EVENT, REQ_HERO, next.eventId)
                )
            }

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
        const val REQ_CALENDAR = 8301
        const val REQ_ADD = 8302
        const val REQ_HERO = 8303
        const val REQ_TEMPLATE = 8304
    }
}
