package com.foksi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.foksi.app.data.local.entity.AttachmentEntity
import com.foksi.app.data.local.entity.CategoryEntity
import com.foksi.app.data.local.entity.ChecklistItemEntity
import com.foksi.app.data.local.entity.EventEntity
import com.foksi.app.data.local.entity.EventWithDetails
import com.foksi.app.data.local.entity.ReminderEntity
import com.foksi.app.data.local.entity.ScheduledNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    fun observeDetails(id: Long): Flow<EventWithDetails?>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getDetails(id: Long): EventWithDetails?

    @Transaction
    @Query("SELECT * FROM events ORDER BY COALESCE(startAt, createdAt) ASC")
    suspend fun getAllDetails(): List<EventWithDetails>

    /**
     * Everything the home and task screens need: anything still open, anything with a date from
     * [from] onwards, plus recently completed items. Bounded so a huge database stays cheap.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM events
        WHERE (startAt IS NOT NULL AND startAt >= :from)
           OR (completed = 0)
           OR (completedAt IS NOT NULL AND completedAt >= :from)
        ORDER BY COALESCE(startAt, createdAt) ASC
        LIMIT :limit
        """
    )
    fun observeAgenda(from: Long, limit: Int): Flow<List<EventWithDetails>>

    @Transaction
    @Query("SELECT * FROM events WHERE startAt IS NOT NULL AND startAt BETWEEN :from AND :to ORDER BY startAt ASC")
    fun observeRange(from: Long, to: Long): Flow<List<EventWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM events
        WHERE title LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR location LIKE '%' || :query || '%'
        ORDER BY COALESCE(startAt, createdAt) DESC
        LIMIT 200
        """
    )
    fun search(query: String): Flow<List<EventWithDetails>>

    @Query("SELECT * FROM events WHERE completed = 0 AND startAt IS NOT NULL AND startAt >= :from ORDER BY startAt ASC LIMIT :limit")
    suspend fun getUpcoming(from: Long, limit: Int): List<EventEntity>

    @Query("SELECT * FROM events WHERE completed = 0 AND type = 'TASK' ORDER BY COALESCE(startAt, createdAt) ASC LIMIT :limit")
    suspend fun getOpenTasks(limit: Int): List<EventEntity>

    @Query("SELECT * FROM events WHERE type = 'NOTE' ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getNotes(limit: Int): List<EventEntity>

    @Transaction
    @Query("SELECT * FROM events WHERE type = 'BIRTHDAY' ORDER BY startAt ASC")
    fun observeBirthdays(): Flow<List<EventWithDetails>>

    @Query("SELECT * FROM events WHERE completed = 0 ORDER BY COALESCE(startAt, createdAt) ASC")
    suspend fun getSchedulable(): List<EventEntity>

    @Query("UPDATE events SET completed = :completed, completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?, now: Long)
}

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)

    @Query("SELECT * FROM reminders WHERE eventId = :eventId")
    suspend fun getForEvent(eventId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?
}

@Dao
interface ChecklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Query("DELETE FROM checklist_items WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)

    @Query("SELECT * FROM checklist_items WHERE eventId = :eventId ORDER BY position ASC")
    suspend fun getForEvent(eventId: Long): List<ChecklistItemEntity>

    @Query("UPDATE checklist_items SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("SELECT COUNT(*) FROM checklist_items WHERE eventId = :eventId AND done = 0")
    suspend fun openCount(eventId: Long): Int
}

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)

    @Query("SELECT * FROM attachments WHERE eventId = :eventId")
    suspend fun getForEvent(eventId: Long): List<AttachmentEntity>
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScheduledNotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduledNotificationEntity>)

    @Query("SELECT * FROM scheduled_notifications WHERE id = :id")
    suspend fun getById(id: Long): ScheduledNotificationEntity?

    @Query("DELETE FROM scheduled_notifications WHERE eventId = :eventId AND fired = 0 AND kind != 'SNOOZE'")
    suspend fun deletePendingForEvent(eventId: Long)

    @Query("DELETE FROM scheduled_notifications WHERE eventId = :eventId")
    suspend fun deleteAllForEvent(eventId: Long)

    @Query("DELETE FROM scheduled_notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scheduled_notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM scheduled_notifications WHERE fired = 1 AND triggerAt < :before")
    suspend fun purgeFiredBefore(before: Long)

    @Query("UPDATE scheduled_notifications SET fired = 1 WHERE id = :id")
    suspend fun markFired(id: Long)

    @Query("UPDATE scheduled_notifications SET triggerAt = :triggerAt WHERE id = :id")
    suspend fun updateTrigger(id: Long, triggerAt: Long)

    @Query("SELECT * FROM scheduled_notifications WHERE fired = 0 AND triggerAt <= :until ORDER BY triggerAt ASC LIMIT :limit")
    suspend fun getPendingUntil(until: Long, limit: Int): List<ScheduledNotificationEntity>

    @Query("SELECT * FROM scheduled_notifications WHERE fired = 0 AND triggerAt > :after ORDER BY triggerAt ASC LIMIT 1")
    suspend fun getFirstAfter(after: Long): ScheduledNotificationEntity?

    @Query("SELECT * FROM scheduled_notifications WHERE fired = 0 ORDER BY triggerAt ASC")
    suspend fun getAllPending(): List<ScheduledNotificationEntity>

    @Query("SELECT * FROM scheduled_notifications WHERE eventId = :eventId AND fired = 0 ORDER BY triggerAt ASC")
    suspend fun getPendingForEvent(eventId: Long): List<ScheduledNotificationEntity>

    @Query("SELECT * FROM scheduled_notifications WHERE fired = 0 AND triggerAt <= :now ORDER BY triggerAt ASC LIMIT 20")
    suspend fun getDue(now: Long): List<ScheduledNotificationEntity>
}
