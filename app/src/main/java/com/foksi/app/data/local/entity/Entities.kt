package com.foksi.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.foksi.app.domain.model.AdvanceMode
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.NotificationKind
import com.foksi.app.domain.model.Priority
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.domain.model.RepeatMode

data class RepeatEmbedded(
    @ColumnInfo(name = "mode") val mode: RepeatMode = RepeatMode.NONE,
    @ColumnInfo(name = "interval") val interval: Int = 1,
    @ColumnInfo(name = "week_days") val weekDays: Set<Int> = emptySet(),
    @ColumnInfo(name = "until") val until: Long? = null,
)

data class AdvanceEmbedded(
    @ColumnInfo(name = "enabled") val enabled: Boolean = false,
    @ColumnInfo(name = "mode") val mode: AdvanceMode = AdvanceMode.CHAIN,
    @ColumnInfo(name = "start_days") val startDaysBefore: Int = 14,
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 3,
    @ColumnInfo(name = "per_week") val perWeek: Int = 3,
    @ColumnInfo(name = "window_start") val windowStartMinutes: Int = 9 * 60,
    @ColumnInfo(name = "window_end") val windowEndMinutes: Int = 21 * 60,
    @ColumnInfo(name = "workdays_only") val workdaysOnly: Boolean = false,
    @ColumnInfo(name = "disabled_steps") val disabledChainSteps: Set<Int> = emptySet(),
    @ColumnInfo(name = "checklist_nudge") val checklistNudge: Boolean = false,
)

@Entity(
    tableName = "events",
    indices = [Index("startAt"), Index("categoryId"), Index("completed")]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ItemType = ItemType.EVENT,
    val title: String = "",
    val description: String = "",
    val notes: String = "",
    val startAt: Long? = null,
    val allDay: Boolean = false,
    val durationMinutes: Int = 60,
    val location: String = "",
    val url: String = "",
    val categoryId: Long? = null,
    val priority: Priority = Priority.NORMAL,
    val colorArgb: Int? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    @Embedded(prefix = "rep_") val repeat: RepeatEmbedded = RepeatEmbedded(),
    @Embedded(prefix = "adv_") val advance: AdvanceEmbedded = AdvanceEmbedded(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val minutesBefore: Int = 15,
    val type: ReminderType = ReminderType.NORMAL,
    val enabled: Boolean = true,
    val repeatUntilAck: Boolean = false,
    val repeatIntervalMinutes: Int = 5,
    val repeatMaxCount: Int = 3,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val text: String = "",
    val done: Boolean = false,
    val position: Int = 0,
)

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val uri: String = "",
    val name: String = "",
    val mime: String = "",
    val isLink: Boolean = false,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val colorArgb: Int = 0xFF9E9E9E.toInt(),
    val builtInKey: String? = null,
)

@Entity(
    tableName = "scheduled_notifications",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId"), Index("triggerAt"), Index("fired")]
)
data class ScheduledNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val reminderId: Long? = null,
    val triggerAt: Long = 0,
    val occurrenceStart: Long = 0,
    val kind: NotificationKind = NotificationKind.MAIN,
    val type: ReminderType = ReminderType.NORMAL,
    val repeatIndex: Int = 0,
    val fired: Boolean = false,
)

data class EventWithDetails(
    @Embedded val event: EventEntity,
    @Relation(parentColumn = "id", entityColumn = "eventId")
    val reminders: List<ReminderEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "eventId")
    val checklist: List<ChecklistItemEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "eventId")
    val attachments: List<AttachmentEntity> = emptyList(),
)
