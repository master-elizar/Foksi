package com.foksi.app.data.repository

import com.foksi.app.data.local.entity.AdvanceEmbedded
import com.foksi.app.data.local.entity.AttachmentEntity
import com.foksi.app.data.local.entity.CategoryEntity
import com.foksi.app.data.local.entity.ChecklistItemEntity
import com.foksi.app.data.local.entity.EventEntity
import com.foksi.app.data.local.entity.EventWithDetails
import com.foksi.app.data.local.entity.ReminderEntity
import com.foksi.app.data.local.entity.RepeatEmbedded
import com.foksi.app.data.local.entity.ScheduledNotificationEntity
import com.foksi.app.domain.model.AdvanceConfig
import com.foksi.app.domain.model.Attachment
import com.foksi.app.domain.model.Category
import com.foksi.app.domain.model.ChecklistItem
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Reminder
import com.foksi.app.domain.model.RepeatRule
import com.foksi.app.domain.model.ScheduledNotification

fun RepeatEmbedded.toDomain() = RepeatRule(mode, interval, weekDays, until)

fun RepeatRule.toEmbedded() = RepeatEmbedded(mode, interval, weekDays, until)

fun AdvanceEmbedded.toDomain() = AdvanceConfig(
    enabled = enabled,
    mode = mode,
    startDaysBefore = startDaysBefore,
    intervalDays = intervalDays,
    perWeek = perWeek,
    windowStartMinutes = windowStartMinutes,
    windowEndMinutes = windowEndMinutes,
    workdaysOnly = workdaysOnly,
    disabledChainSteps = disabledChainSteps,
    checklistNudge = checklistNudge,
)

fun AdvanceConfig.toEmbedded() = AdvanceEmbedded(
    enabled = enabled,
    mode = mode,
    startDaysBefore = startDaysBefore,
    intervalDays = intervalDays,
    perWeek = perWeek,
    windowStartMinutes = windowStartMinutes,
    windowEndMinutes = windowEndMinutes,
    workdaysOnly = workdaysOnly,
    disabledChainSteps = disabledChainSteps,
    checklistNudge = checklistNudge,
)

fun EventEntity.toDomain() = PlanItem(
    id = id,
    type = type,
    title = title,
    description = description,
    notes = notes,
    startAt = startAt,
    allDay = allDay,
    durationMinutes = durationMinutes,
    location = location,
    url = url,
    categoryId = categoryId,
    priority = priority,
    colorArgb = colorArgb,
    completed = completed,
    completedAt = completedAt,
    repeat = repeat.toDomain(),
    advance = advance.toDomain(),
    birthYearKnown = birthYearKnown,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PlanItem.toEntity() = EventEntity(
    id = id,
    type = type,
    title = title,
    description = description,
    notes = notes,
    startAt = startAt,
    allDay = allDay,
    durationMinutes = durationMinutes,
    location = location,
    url = url,
    categoryId = categoryId,
    priority = priority,
    colorArgb = colorArgb,
    completed = completed,
    completedAt = completedAt,
    repeat = repeat.toEmbedded(),
    advance = advance.toEmbedded(),
    birthYearKnown = birthYearKnown,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ReminderEntity.toDomain() = Reminder(
    id = id,
    eventId = eventId,
    minutesBefore = minutesBefore,
    type = type,
    enabled = enabled,
    repeatUntilAck = repeatUntilAck,
    repeatIntervalMinutes = repeatIntervalMinutes,
    repeatMaxCount = repeatMaxCount,
)

fun Reminder.toEntity(ownerId: Long = eventId) = ReminderEntity(
    id = id,
    eventId = ownerId,
    minutesBefore = minutesBefore,
    type = type,
    enabled = enabled,
    repeatUntilAck = repeatUntilAck,
    repeatIntervalMinutes = repeatIntervalMinutes,
    repeatMaxCount = repeatMaxCount,
)

fun ChecklistItemEntity.toDomain() = ChecklistItem(id, eventId, text, done, position)

fun ChecklistItem.toEntity(ownerId: Long = eventId) =
    ChecklistItemEntity(id, ownerId, text, done, position)

fun AttachmentEntity.toDomain() = Attachment(id, eventId, uri, name, mime, isLink)

fun Attachment.toEntity(ownerId: Long = eventId) =
    AttachmentEntity(id, ownerId, uri, name, mime, isLink)

fun CategoryEntity.toDomain() = Category(id, name, colorArgb, builtInKey)

fun Category.toEntity() = CategoryEntity(id, name, colorArgb, builtInKey)

fun ScheduledNotificationEntity.toDomain() = ScheduledNotification(
    id = id,
    eventId = eventId,
    reminderId = reminderId,
    triggerAt = triggerAt,
    occurrenceStart = occurrenceStart,
    kind = kind,
    type = type,
    repeatIndex = repeatIndex,
    fired = fired,
)

fun ScheduledNotification.toEntity() = ScheduledNotificationEntity(
    id = id,
    eventId = eventId,
    reminderId = reminderId,
    triggerAt = triggerAt,
    occurrenceStart = occurrenceStart,
    kind = kind,
    type = type,
    repeatIndex = repeatIndex,
    fired = fired,
)

fun EventWithDetails.toDomain() = PlanItemDetails(
    item = event.toDomain(),
    reminders = reminders.map { it.toDomain() }.sortedByDescending { it.minutesBefore },
    checklist = checklist.map { it.toDomain() }.sortedBy { it.position },
    attachments = attachments.map { it.toDomain() },
)
