package com.foksi.app.data.backup

import android.content.Context
import android.net.Uri
import com.foksi.app.core.TimeUtils
import com.foksi.app.data.repository.PlanRepository
import com.foksi.app.domain.model.AdvanceConfig
import com.foksi.app.domain.model.AdvanceMode
import com.foksi.app.domain.model.Attachment
import com.foksi.app.domain.model.ChecklistItem
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Priority
import com.foksi.app.domain.model.Reminder
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.domain.model.RepeatMode
import com.foksi.app.domain.model.RepeatRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Local export, import and backup. JSON keeps everything Foksi knows; ICS is there so events can
 * travel to any other calendar app.
 */
class BackupManager(
    private val context: Context,
    private val repository: PlanRepository,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val icsFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    suspend fun exportJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        val payload = buildBackup()
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(json.encodeToString(BackupFile.serializer(), payload).toByteArray())
        } ?: error("Cannot open $uri")
        payload.items.size
    }

    suspend fun exportIcs(uri: Uri): Int = withContext(Dispatchers.IO) {
        val details = repository.getAllDetails().filter { it.item.startAt != null }
        val text = buildIcs(details)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
            ?: error("Cannot open $uri")
        details.size
    }

    suspend fun import(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: error("Cannot read $uri")
        importText(text)
    }

    suspend fun importText(text: String): Int {
        val trimmed = text.trim()
        return if (trimmed.startsWith("BEGIN:VCALENDAR", ignoreCase = true)) {
            importIcs(trimmed)
        } else {
            importJson(trimmed)
        }
    }

    /** Writes a backup inside the app's own storage, so no permission dialog is needed. */
    suspend fun backupToInternal(): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val file = File(dir, "foksi-backup.json")
        file.writeText(json.encodeToString(BackupFile.serializer(), buildBackup()))
        file
    }

    suspend fun restoreFromInternal(): Int = withContext(Dispatchers.IO) {
        val file = File(File(context.filesDir, "backups"), "foksi-backup.json")
        if (!file.exists()) 0 else importJson(file.readText())
    }

    fun hasInternalBackup(): Boolean =
        File(File(context.filesDir, "backups"), "foksi-backup.json").exists()

    private suspend fun buildBackup(): BackupFile {
        val categories = repository.getCategories().associateBy { it.id }
        val items = repository.getAllDetails().map { details ->
            val item = details.item
            BackupItem(
                type = item.type.name,
                title = item.title,
                description = item.description,
                notes = item.notes,
                startAt = item.startAt,
                allDay = item.allDay,
                durationMinutes = item.durationMinutes,
                location = item.location,
                url = item.url,
                priority = item.priority.name,
                colorArgb = item.colorArgb,
                completed = item.completed,
                categoryName = item.categoryId?.let { categories[it]?.name },
                repeatMode = item.repeat.mode.name,
                repeatInterval = item.repeat.interval,
                repeatWeekDays = item.repeat.weekDays.toList(),
                repeatUntil = item.repeat.until,
                advance = BackupAdvance(
                    enabled = item.advance.enabled,
                    mode = item.advance.mode.name,
                    startDaysBefore = item.advance.startDaysBefore,
                    intervalDays = item.advance.intervalDays,
                    perWeek = item.advance.perWeek,
                    windowStartMinutes = item.advance.windowStartMinutes,
                    windowEndMinutes = item.advance.windowEndMinutes,
                    workdaysOnly = item.advance.workdaysOnly,
                    disabledChainSteps = item.advance.disabledChainSteps.toList(),
                    checklistNudge = item.advance.checklistNudge,
                ),
                reminders = details.reminders.map {
                    BackupReminder(
                        it.minutesBefore, it.type.name, it.enabled,
                        it.repeatUntilAck, it.repeatIntervalMinutes, it.repeatMaxCount
                    )
                },
                checklist = details.checklist.map { BackupChecklistItem(it.text, it.done) },
                attachments = details.attachments.map {
                    BackupAttachment(it.uri, it.name, it.mime, it.isLink)
                },
            )
        }
        return BackupFile(version = 1, exportedAt = TimeUtils.now(), items = items)
    }

    private suspend fun importJson(text: String): Int {
        val parsed = json.decodeFromString(BackupFile.serializer(), text)
        val categories = repository.getCategories().associateBy { it.name.lowercase() }
        var count = 0
        parsed.items.forEach { backup ->
            val categoryId = backup.categoryName?.let { categories[it.lowercase()]?.id }
            val advance = backup.advance
            val details = PlanItemDetails(
                item = PlanItem(
                    type = enumOrDefault(backup.type, ItemType.EVENT),
                    title = backup.title,
                    description = backup.description,
                    notes = backup.notes,
                    startAt = backup.startAt,
                    allDay = backup.allDay,
                    durationMinutes = backup.durationMinutes,
                    location = backup.location,
                    url = backup.url,
                    categoryId = categoryId,
                    priority = enumOrDefault(backup.priority, Priority.NORMAL),
                    colorArgb = backup.colorArgb,
                    completed = backup.completed,
                    repeat = RepeatRule(
                        mode = enumOrDefault(backup.repeatMode, RepeatMode.NONE),
                        interval = backup.repeatInterval,
                        weekDays = backup.repeatWeekDays.toSet(),
                        until = backup.repeatUntil,
                    ),
                    advance = AdvanceConfig(
                        enabled = advance?.enabled ?: false,
                        mode = enumOrDefault(advance?.mode ?: "CHAIN", AdvanceMode.CHAIN),
                        startDaysBefore = advance?.startDaysBefore ?: 14,
                        intervalDays = advance?.intervalDays ?: 3,
                        perWeek = advance?.perWeek ?: 3,
                        windowStartMinutes = advance?.windowStartMinutes ?: 540,
                        windowEndMinutes = advance?.windowEndMinutes ?: 1260,
                        workdaysOnly = advance?.workdaysOnly ?: false,
                        disabledChainSteps = advance?.disabledChainSteps?.toSet() ?: emptySet(),
                        checklistNudge = advance?.checklistNudge ?: false,
                    ),
                ),
                reminders = backup.reminders.map {
                    Reminder(
                        minutesBefore = it.minutesBefore,
                        type = enumOrDefault(it.type, ReminderType.NORMAL),
                        enabled = it.enabled,
                        repeatUntilAck = it.repeatUntilAck,
                        repeatIntervalMinutes = it.repeatIntervalMinutes,
                        repeatMaxCount = it.repeatMaxCount,
                    )
                },
                checklist = backup.checklist.mapIndexed { index, c ->
                    ChecklistItem(text = c.text, done = c.done, position = index)
                },
                attachments = backup.attachments.map {
                    Attachment(uri = it.uri, name = it.name, mime = it.mime, isLink = it.isLink)
                },
            )
            repository.save(details)
            count++
        }
        return count
    }

    private fun buildIcs(items: List<PlanItemDetails>): String = buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//Foksi//Planner//EN")
        appendLine("CALSCALE:GREGORIAN")
        items.forEach { details ->
            val item = details.item
            val start = item.startAt ?: return@forEach
            appendLine("BEGIN:VEVENT")
            appendLine("UID:foksi-${item.id}@foksi.app")
            appendLine("DTSTAMP:${icsFormat.format(Instant.ofEpochMilli(TimeUtils.now()))}")
            appendLine("DTSTART:${icsFormat.format(Instant.ofEpochMilli(start))}")
            appendLine("DTEND:${icsFormat.format(Instant.ofEpochMilli(item.endAt ?: start))}")
            appendLine("SUMMARY:${escape(item.title)}")
            if (item.description.isNotBlank() || item.notes.isNotBlank()) {
                appendLine("DESCRIPTION:${escape(listOf(item.description, item.notes).filter { it.isNotBlank() }.joinToString("\n"))}")
            }
            if (item.location.isNotBlank()) appendLine("LOCATION:${escape(item.location)}")
            if (item.url.isNotBlank()) appendLine("URL:${escape(item.url)}")
            rruleFor(item)?.let { appendLine(it) }
            details.reminders.filter { it.enabled }.forEach { reminder ->
                appendLine("BEGIN:VALARM")
                appendLine("ACTION:DISPLAY")
                appendLine("DESCRIPTION:${escape(item.title)}")
                appendLine("TRIGGER:-PT${reminder.minutesBefore}M")
                appendLine("END:VALARM")
            }
            appendLine("END:VEVENT")
        }
        appendLine("END:VCALENDAR")
    }

    private fun rruleFor(item: PlanItem): String? = when (item.repeat.mode) {
        RepeatMode.NONE -> null
        RepeatMode.DAILY, RepeatMode.CUSTOM_DAYS -> "RRULE:FREQ=DAILY;INTERVAL=${item.repeat.interval}"
        RepeatMode.WEEKLY -> {
            val days = item.repeat.weekDays.mapNotNull { dayCode(it) }.joinToString(",")
            if (days.isBlank()) "RRULE:FREQ=WEEKLY;INTERVAL=${item.repeat.interval}"
            else "RRULE:FREQ=WEEKLY;INTERVAL=${item.repeat.interval};BYDAY=$days"
        }
        RepeatMode.WEEKDAYS -> "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
        RepeatMode.MONTHLY -> "RRULE:FREQ=MONTHLY;INTERVAL=${item.repeat.interval}"
        RepeatMode.YEARLY -> "RRULE:FREQ=YEARLY;INTERVAL=${item.repeat.interval}"
    }

    private fun dayCode(value: Int): String? = when (value) {
        1 -> "MO"; 2 -> "TU"; 3 -> "WE"; 4 -> "TH"; 5 -> "FR"; 6 -> "SA"; 7 -> "SU"
        else -> null
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace(",", "\\,")
        .replace(";", "\;")

    private fun unescape(value: String): String = value
        .replace("\\n", "\n")
        .replace("\\,", ",")
        .replace("\;", ";")
        .replace("\\\\", "\\")

    private suspend fun importIcs(text: String): Int {
        val unfolded = text.replace("\r\n ", "").replace("\n ", "").replace("\r\n", "\n")
        var count = 0
        var inEvent = false
        var summary = ""
        var description = ""
        var location = ""
        var url = ""
        var start: Long? = null
        var end: Long? = null
        val alarms = mutableListOf<Int>()

        unfolded.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.equals("BEGIN:VEVENT", true) -> {
                    inEvent = true
                    summary = ""; description = ""; location = ""; url = ""
                    start = null; end = null; alarms.clear()
                }

                line.equals("END:VEVENT", true) && inEvent -> {
                    inEvent = false
                    val startAt = start
                    if (summary.isNotBlank() || startAt != null) {
                        val duration = if (startAt != null && end != null && end!! > startAt) {
                            ((end!! - startAt) / 60_000L).toInt().coerceIn(5, 60 * 24)
                        } else {
                            60
                        }
                        repository.save(
                            PlanItemDetails(
                                item = PlanItem(
                                    type = if (startAt == null) ItemType.TASK else ItemType.EVENT,
                                    title = summary.ifBlank { "Event" },
                                    description = description,
                                    startAt = startAt,
                                    durationMinutes = duration,
                                    location = location,
                                    url = url,
                                ),
                                reminders = alarms.distinct().map {
                                    Reminder(minutesBefore = it, type = ReminderType.NORMAL)
                                },
                            )
                        )
                        count++
                    }
                }

                !inEvent -> Unit
                line.startsWith("SUMMARY", true) -> summary = unescape(line.substringAfter(':', ""))
                line.startsWith("DESCRIPTION", true) -> description = unescape(line.substringAfter(':', ""))
                line.startsWith("LOCATION", true) -> location = unescape(line.substringAfter(':', ""))
                line.startsWith("URL", true) -> url = unescape(line.substringAfter(':', ""))
                line.startsWith("DTSTART", true) -> start = parseIcsDate(line)
                line.startsWith("DTEND", true) -> end = parseIcsDate(line)
                line.startsWith("TRIGGER", true) -> parseTrigger(line)?.let { alarms += it }
            }
        }
        return count
    }

    private fun parseIcsDate(line: String): Long? {
        val value = line.substringAfter(':', "").trim()
        if (value.isEmpty()) return null
        return runCatching {
            when {
                value.endsWith("Z") -> Instant.from(icsFormat.parse(value)).toEpochMilli()
                value.length == 8 -> TimeUtils.toMillis(
                    java.time.LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE),
                    java.time.LocalTime.of(9, 0)
                )
                else -> {
                    val local = java.time.LocalDateTime.parse(
                        value, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                    )
                    TimeUtils.toMillis(local)
                }
            }
        }.getOrNull()
    }

    /** Supports the common "-PT30M" / "-P1D" style triggers. */
    private fun parseTrigger(line: String): Int? {
        val value = line.substringAfter(':', "").trim().uppercase()
        if (!value.startsWith("-P")) return null
        val body = value.removePrefix("-P")
        val days = Regex("(\\d+)D").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val timePart = body.substringAfter('T', "")
        val hours = Regex("(\\d+)H").find(timePart)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = Regex("(\\d+)M").find(timePart)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val weeks = Regex("(\\d+)W").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = weeks * 7 * 24 * 60 + days * 24 * 60 + hours * 60 + minutes
        return if (total == 0) null else total
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
}
