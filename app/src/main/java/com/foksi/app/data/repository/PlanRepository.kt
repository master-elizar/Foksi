package com.foksi.app.data.repository

import androidx.room.withTransaction
import com.foksi.app.core.TimeUtils
import com.foksi.app.data.local.FoksiDatabase
import com.foksi.app.domain.model.Category
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.ScheduledNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single door to stored plans. Everything above this class works with domain models only.
 */
class PlanRepository(private val db: FoksiDatabase) {

    private val events = db.eventDao()
    private val reminders = db.reminderDao()
    private val checklist = db.checklistDao()
    private val attachments = db.attachmentDao()
    private val categories = db.categoryDao()
    private val schedule = db.scheduleDao()

    fun observeAgenda(from: Long = TimeUtils.startOfDay(TimeUtils.now()), limit: Int = 500): Flow<List<PlanItemDetails>> =
        events.observeAgenda(from, limit).map { list -> list.map { it.toDomain() } }

    fun observeRange(from: Long, to: Long): Flow<List<PlanItemDetails>> =
        events.observeRange(from, to).map { list -> list.map { it.toDomain() } }

    fun observeDetails(id: Long): Flow<PlanItemDetails?> =
        events.observeDetails(id).map { it?.toDomain() }

    fun search(query: String): Flow<List<PlanItemDetails>> =
        events.search(query).map { list -> list.map { it.toDomain() } }

    fun observeBirthdays(): Flow<List<PlanItemDetails>> =
        events.observeBirthdays().map { list -> list.map { it.toDomain() } }

    fun observeCategories(): Flow<List<Category>> =
        categories.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getDetails(id: Long): PlanItemDetails? = events.getDetails(id)?.toDomain()

    suspend fun getItem(id: Long): PlanItem? = events.getById(id)?.toDomain()

    suspend fun getAllDetails(): List<PlanItemDetails> = events.getAllDetails().map { it.toDomain() }

    suspend fun getSchedulable(): List<PlanItem> = events.getSchedulable().map { it.toDomain() }

    suspend fun getUpcoming(from: Long, limit: Int): List<PlanItem> =
        events.getUpcoming(from, limit).map { it.toDomain() }

    suspend fun getOpenTasks(limit: Int): List<PlanItem> =
        events.getOpenTasks(limit).map { it.toDomain() }

    suspend fun getNotes(limit: Int): List<PlanItem> = events.getNotes(limit).map { it.toDomain() }

    suspend fun getCategories(): List<Category> = categories.getAll().map { it.toDomain() }

    suspend fun openChecklistCount(eventId: Long): Int = checklist.openCount(eventId)

    /** Inserts or updates an item together with all of its children in one transaction. */
    suspend fun save(details: PlanItemDetails): Long = db.withTransaction {
        val now = TimeUtils.now()
        val item = details.item
        val id: Long
        if (item.id == 0L) {
            id = events.insert(item.copy(createdAt = now, updatedAt = now).toEntity())
        } else {
            id = item.id
            events.update(item.copy(updatedAt = now).toEntity())
        }
        reminders.deleteForEvent(id)
        reminders.insertAll(details.reminders.map { it.copy(id = 0).toEntity(id) })
        checklist.deleteForEvent(id)
        checklist.insertAll(
            details.checklist.mapIndexed { index, c -> c.copy(id = 0, position = index).toEntity(id) }
        )
        attachments.deleteForEvent(id)
        attachments.insertAll(details.attachments.map { it.copy(id = 0).toEntity(id) })
        id
    }

    suspend fun quickCreate(
        type: ItemType,
        title: String,
        body: String = "",
    ): Long = save(
        PlanItemDetails(
            item = PlanItem(
                type = type,
                title = title,
                // A note keeps its body as notes; a task keeps it as the description.
                notes = if (type == ItemType.NOTE) body else "",
                description = if (type == ItemType.NOTE) "" else body,
            )
        )
    )

    suspend fun delete(id: Long) = db.withTransaction {
        schedule.deleteAllForEvent(id)
        events.delete(id)
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        val now = TimeUtils.now()
        events.setCompleted(id, completed, if (completed) now else null, now)
    }

    suspend fun setChecklistDone(itemId: Long, done: Boolean) = checklist.setDone(itemId, done)

    suspend fun duplicate(id: Long): Long? {
        val details = getDetails(id) ?: return null
        val copy = details.copy(
            item = details.item.copy(id = 0, completed = false, completedAt = null),
            reminders = details.reminders.map { it.copy(id = 0, eventId = 0) },
            checklist = details.checklist.map { it.copy(id = 0, eventId = 0, done = false) },
            attachments = details.attachments.map { it.copy(id = 0, eventId = 0) },
        )
        return save(copy)
    }

    suspend fun addCategory(name: String, colorArgb: Int): Long =
        categories.insert(Category(name = name, colorArgb = colorArgb).toEntity())

    suspend fun deleteCategory(id: Long) = categories.delete(id)

    // --- scheduled notifications -------------------------------------------------------------

    suspend fun replacePending(eventId: Long, items: List<ScheduledNotification>) = db.withTransaction {
        schedule.deletePendingForEvent(eventId)
        schedule.insertAll(items.map { it.toEntity() })
    }

    suspend fun insertScheduled(item: ScheduledNotification): Long = schedule.insert(item.toEntity())

    suspend fun getScheduled(id: Long): ScheduledNotification? = schedule.getById(id)?.toDomain()

    suspend fun getPendingUntil(until: Long, limit: Int = 400): List<ScheduledNotification> =
        schedule.getPendingUntil(until, limit).map { it.toDomain() }

    suspend fun getFirstPendingAfter(after: Long): ScheduledNotification? =
        schedule.getFirstAfter(after)?.toDomain()

    suspend fun getPendingForEvent(eventId: Long): List<ScheduledNotification> =
        schedule.getPendingForEvent(eventId).map { it.toDomain() }

    suspend fun getAllPending(): List<ScheduledNotification> =
        schedule.getAllPending().map { it.toDomain() }

    suspend fun getDue(now: Long): List<ScheduledNotification> = schedule.getDue(now).map { it.toDomain() }

    suspend fun markFired(id: Long) = schedule.markFired(id)

    suspend fun updateTrigger(id: Long, triggerAt: Long) = schedule.updateTrigger(id, triggerAt)

    suspend fun deleteScheduled(id: Long) = schedule.delete(id)

    suspend fun purgeOldSchedules(before: Long) = schedule.purgeFiredBefore(before)

    suspend fun clearAll() = db.withTransaction {
        schedule.deleteAll()
        events.deleteAll()
    }
}
