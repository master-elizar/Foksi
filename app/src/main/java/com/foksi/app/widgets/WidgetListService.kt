package com.foksi.app.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.foksi.app.R
import com.foksi.app.notifications.Extras

/** Feeds the scrolling list inside the 4×2 and 4×4 widgets. */
class WidgetListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        FoksiListFactory(applicationContext, intent.getIntExtra(EXTRA_LIMIT, 6))

    companion object {
        const val EXTRA_LIMIT = "limit"
    }
}

private class FoksiListFactory(
    private val context: Context,
    private val limit: Int,
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<WidgetRow> = emptyList()

    override fun onCreate() {
        rows = loadRows()
    }

    override fun onDataSetChanged() {
        rows = loadRows()
    }

    private fun loadRows(): List<WidgetRow> =
        runCatching { WidgetData.load(context, limit).rows }.getOrDefault(emptyList())

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_item)
        return RemoteViews(context.packageName, R.layout.widget_item).apply {
            if (row.group != null) {
                setViewVisibility(R.id.item_group, View.VISIBLE)
                setTextViewText(R.id.item_group, row.group)
            } else {
                setViewVisibility(R.id.item_group, View.GONE)
            }
            setTextViewText(R.id.item_title, row.title)
            setTextViewText(R.id.item_subtitle, row.subtitle)
            setTextViewText(R.id.item_countdown, row.countdown)
            setViewVisibility(
                R.id.item_countdown,
                if (row.countdown.isBlank()) View.GONE else View.VISIBLE
            )
            setInt(R.id.item_dot, "setColorFilter", row.colorArgb)
            setOnClickFillInIntent(
                R.id.item_card,
                Intent().putExtra(Extras.EXTRA_EVENT_ID, row.eventId)
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.eventId ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}

internal fun listServiceIntent(context: Context, appWidgetId: Int, limit: Int): Intent =
    Intent(context, WidgetListService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(WidgetListService.EXTRA_LIMIT, limit)
        data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }
