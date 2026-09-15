package com.foksi.app.widgets

import android.content.Context
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.logic.Agenda
import com.foksi.app.domain.logic.AgendaGroup
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Priority
import kotlinx.coroutines.runBlocking

data class WidgetRow(
    val eventId: Long,
    val group: String?,
    val title: String,
    val subtitle: String,
    val countdown: String,
    val colorArgb: Int,
)

data class WidgetSnapshot(
    val header: String,
    val next: WidgetRow?,
    val rows: List<WidgetRow>,
)

/**
 * Widgets run in the launcher process on a binder thread, so the data is loaded synchronously
 * here — the queries are bounded and indexed, which keeps this well below the ANR budget.
 */
object WidgetData {

    fun load(context: Context, limit: Int? = null): WidgetSnapshot = runBlocking {
        AppGraph.init(context)
        val settings = AppGraph.settingsRepository.current()
        val now = TimeUtils.now()
        val max = limit ?: settings.widgetEventCount
        val repository = AppGraph.planRepository

        val details = mutableListOf<PlanItemDetails>()
        repository.getUpcoming(TimeUtils.startOfDay(now), max * 3).forEach { item ->
            repository.getDetails(item.id)?.let { details += it }
        }
        repository.getOpenTasks(max).forEach { item ->
            if (details.none { it.item.id == item.id }) {
                repository.getDetails(item.id)?.let { details += it }
            }
        }
        if (settings.widgetShowNotes) {
            repository.getNotes(3).forEach { item ->
                repository.getDetails(item.id)?.let { details += it }
            }
        }

        val visible = details.filter { settings.widgetShowCompleted || !it.item.completed }
        val sections = Agenda.sections(visible, now)
        val rows = mutableListOf<WidgetRow>()
        sections.forEach { section ->
            if (section.group == AgendaGroup.COMPLETED && !settings.widgetShowCompleted) return@forEach
            var first = true
            section.items.forEach { item ->
                if (rows.size >= max) return@forEach
                rows += toRow(context, item, settings, now, if (first) groupLabel(context, section.group) else null)
                first = false
            }
        }

        val next = Agenda.nextUp(visible, now)?.let { toRow(context, it, settings, now, null) }
        WidgetSnapshot(
            header = TimeUtils.formatWeekdayDate(context, now),
            next = next,
            rows = rows,
        )
    }

    private fun toRow(
        context: Context,
        details: PlanItemDetails,
        settings: AppSettings,
        now: Long,
        group: String?,
    ): WidgetRow {
        val item = details.item
        val start = Agenda.displayStart(details, now)
        val subtitle = when {
            item.type == ItemType.NOTE -> item.notes.lineSequence().firstOrNull().orEmpty()
            start == null -> context.getString(R.string.group_no_date)
            item.allDay -> TimeUtils.formatDate(context, start)
            else -> TimeUtils.formatDateTime(context, start, settings.use24h)
        }
        val countdown = if (settings.widgetShowCountdown && start != null && start > now) {
            TimeUtils.formatCountdownShort(context, start, now)
        } else {
            ""
        }
        val color = item.colorArgb ?: when (item.priority) {
            Priority.CRITICAL -> 0xFFD2496B.toInt()
            Priority.IMPORTANT -> 0xFFE9A13B.toInt()
            Priority.NORMAL -> 0xFF8AA398.toInt()
        }
        return WidgetRow(
            eventId = item.id,
            group = group,
            title = item.title,
            subtitle = subtitle,
            countdown = countdown,
            colorArgb = color,
        )
    }

    fun groupLabel(context: Context, group: AgendaGroup): String = context.getString(
        when (group) {
            AgendaGroup.OVERDUE -> R.string.group_overdue
            AgendaGroup.TODAY -> R.string.group_today
            AgendaGroup.TOMORROW -> R.string.group_tomorrow
            AgendaGroup.THIS_WEEK -> R.string.group_this_week
            AgendaGroup.NEXT_WEEK -> R.string.group_next_week
            AgendaGroup.THIS_MONTH -> R.string.group_this_month
            AgendaGroup.LATER -> R.string.group_later
            AgendaGroup.NO_DATE -> R.string.group_no_date
            AgendaGroup.COMPLETED -> R.string.group_completed
        }
    )
}
