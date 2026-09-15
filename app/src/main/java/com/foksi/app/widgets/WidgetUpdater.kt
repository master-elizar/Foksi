package com.foksi.app.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.foksi.app.R

/** One call refreshes every Foksi widget on the home screen. */
object WidgetUpdater {

    private val providers = listOf(
        NextEventWidgetProvider::class.java,
        UpcomingWidgetProvider::class.java,
        OverviewWidgetProvider::class.java,
    )

    fun requestUpdate(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        providers.forEach { provider ->
            val ids = runCatching {
                manager.getAppWidgetIds(ComponentName(context, provider))
            }.getOrNull() ?: return@forEach
            if (ids.isEmpty()) return@forEach
            if (provider != NextEventWidgetProvider::class.java) {
                runCatching { manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list) }
            }
            context.sendBroadcast(
                Intent(context, provider).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
        }
    }
}
