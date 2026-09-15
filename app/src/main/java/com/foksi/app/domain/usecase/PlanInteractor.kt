package com.foksi.app.domain.usecase

import android.content.Context
import com.foksi.app.data.repository.PlanRepository
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.notifications.ReminderScheduler
import com.foksi.app.widgets.WidgetUpdater

/**
 * Write operations always travel through here so that storing data, re-planning reminders and
 * refreshing widgets can never drift apart.
 */
class PlanInteractor(
    private val context: Context,
    private val repository: PlanRepository,
    private val scheduler: ReminderScheduler,
) {

    suspend fun save(details: PlanItemDetails): Long {
        val id = repository.save(details)
        cancelSystemAlarms(id)
        scheduler.rescheduleEvent(id)
        WidgetUpdater.requestUpdate(context)
        return id
    }

    suspend fun quickNote(text: String): Long {
        val title = text.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { text.trim() }
        val id = repository.quickCreate(ItemType.NOTE, title.take(120), text.trim())
        WidgetUpdater.requestUpdate(context)
        return id
    }

    suspend fun quickTask(text: String, description: String = ""): Long {
        val id = repository.quickCreate(ItemType.TASK, text.trim(), description.trim())
        WidgetUpdater.requestUpdate(context)
        return id
    }

    suspend fun delete(id: Long) {
        cancelSystemAlarms(id)
        repository.delete(id)
        scheduler.syncAlarms()
        WidgetUpdater.requestUpdate(context)
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        repository.setCompleted(id, completed)
        cancelSystemAlarms(id)
        scheduler.rescheduleEvent(id)
        WidgetUpdater.requestUpdate(context)
    }

    suspend fun setChecklistDone(itemId: Long, done: Boolean) {
        repository.setChecklistDone(itemId, done)
        WidgetUpdater.requestUpdate(context)
    }

    suspend fun duplicate(id: Long): Long? {
        val copy = repository.duplicate(id) ?: return null
        scheduler.rescheduleEvent(copy)
        WidgetUpdater.requestUpdate(context)
        return copy
    }

    suspend fun rescheduleEverything() {
        scheduler.rescheduleAll()
        WidgetUpdater.requestUpdate(context)
    }

    private suspend fun cancelSystemAlarms(eventId: Long) {
        repository.getPendingForEvent(eventId).forEach { scheduler.cancel(it.id) }
    }
}
