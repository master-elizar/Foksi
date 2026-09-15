package com.foksi.app.ui.screens.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.logic.Agenda
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.Priority
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.notifications.ReminderStatusChecker
import com.foksi.app.ui.components.LabelValueRow
import com.foksi.app.ui.components.ReminderStatusBanner
import com.foksi.app.ui.components.SectionHeader
import com.foksi.app.ui.vm.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    eventId: Long,
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenItem: (Long) -> Unit,
) {
    LaunchedEffect(eventId) { viewModel.load(eventId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val details = state.details

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(typeLabel(details?.item?.type)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(eventId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { viewModel.duplicate(onOpenItem) }) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.action_duplicate)
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (details == null) {
            Spacer(Modifier.height(1.dp))
            return@Scaffold
        }
        val item = details.item
        val start = Agenda.displayStart(details, TimeUtils.now())

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 40.dp,
                start = 20.dp,
                end = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "hero") {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (start != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (item.allDay) TimeUtils.formatDate(context, start)
                                else TimeUtils.formatDateTime(context, start, state.settings.use24h),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = TimeUtils.formatCountdown(context, start),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            item(key = "status") {
                ReminderStatusBanner(
                    ReminderStatusChecker.check(
                        context,
                        state.settings,
                        needsExactAlarm = details.reminders.any { it.type == ReminderType.ALARM },
                    )
                )
            }

            item(key = "facts") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LabelValueRow(
                            stringResource(R.string.detail_status),
                            when {
                                item.completed -> stringResource(R.string.detail_status_done)
                                start != null && start < TimeUtils.now() ->
                                    stringResource(R.string.detail_status_overdue)
                                else -> stringResource(R.string.detail_status_active)
                            }
                        )
                        LabelValueRow(
                            stringResource(R.string.field_priority),
                            stringResource(priorityLabel(item.priority))
                        )
                        if (item.durationMinutes > 0 && item.type == ItemType.EVENT) {
                            LabelValueRow(
                                stringResource(R.string.field_duration),
                                TimeUtils.formatDuration(context, item.durationMinutes)
                            )
                        }
                        if (item.location.isNotBlank()) {
                            LabelValueRow(stringResource(R.string.field_location), item.location)
                        }
                        if (item.repeat.isRepeating) {
                            LabelValueRow(
                                stringResource(R.string.section_repeat),
                                stringResource(repeatLabel(item.repeat.mode))
                            )
                        }
                        val next = state.upcomingReminders.firstOrNull()
                        LabelValueRow(
                            stringResource(R.string.section_reminders),
                            if (next != null) {
                                TimeUtils.formatDateTime(context, next.triggerAt, state.settings.use24h)
                            } else {
                                stringResource(R.string.detail_no_next_reminder)
                            }
                        )
                    }
                }
            }

            if (item.url.isNotBlank()) {
                item(key = "url") {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(item.url))
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(item.url, maxLines = 1) }
                }
            }

            if (item.description.isNotBlank()) {
                item(key = "description") {
                    SectionHeader(stringResource(R.string.field_description))
                    Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (item.notes.isNotBlank()) {
                item(key = "notes") {
                    SectionHeader(stringResource(R.string.field_notes))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            if (details.checklist.isNotEmpty()) {
                item(key = "checklist-header") {
                    SectionHeader(
                        stringResource(
                            R.string.checklist_progress,
                            details.checklistDone,
                            details.checklistTotal
                        )
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (details.checklistTotal == 0) 0f
                            else details.checklistDone.toFloat() / details.checklistTotal
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                    )
                }
                items(details.checklist.size, key = { "c-${details.checklist[it].id}" }) { index ->
                    val entry = details.checklist[index]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = entry.done,
                            onCheckedChange = { viewModel.toggleChecklistItem(entry.id, it) },
                        )
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (entry.done) {
                                androidx.compose.ui.text.style.TextDecoration.LineThrough
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            if (details.reminders.isNotEmpty()) {
                item(key = "reminders") {
                    SectionHeader(stringResource(R.string.section_reminders))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        details.reminders.forEach { reminder ->
                            LabelValueRow(
                                label = if (reminder.minutesBefore <= 0) {
                                    stringResource(R.string.reminder_at_time)
                                } else {
                                    stringResource(
                                        R.string.reminder_before,
                                        TimeUtils.formatOffset(context, reminder.minutesBefore)
                                    )
                                },
                                value = stringResource(reminderTypeLabel(reminder.type)),
                            )
                        }
                    }
                }
            }

            if (details.attachments.isNotEmpty()) {
                item(key = "attachments") {
                    SectionHeader(stringResource(R.string.section_attachments))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        details.attachments.forEach { attachment ->
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                android.net.Uri.parse(attachment.uri)
                                            ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(attachment.name.ifBlank { attachment.uri }, maxLines = 1) }
                        }
                    }
                }
            }

            item(key = "actions") {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.setCompleted(!item.completed) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                if (item.completed) R.string.action_undone else R.string.action_done
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = { onEdit(eventId) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.action_edit)) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_confirm)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun typeLabel(type: ItemType?): String = stringResource(
    when (type) {
        ItemType.TASK -> R.string.type_task
        ItemType.NOTE -> R.string.type_note
        else -> R.string.type_event
    }
)

fun priorityLabel(priority: Priority): Int = when (priority) {
    Priority.NORMAL -> R.string.priority_normal
    Priority.IMPORTANT -> R.string.priority_important
    Priority.CRITICAL -> R.string.priority_critical
}

fun reminderTypeLabel(type: ReminderType): Int = when (type) {
    ReminderType.NORMAL -> R.string.reminder_type_normal
    ReminderType.IMPORTANT -> R.string.reminder_type_important
    ReminderType.ALARM -> R.string.reminder_type_alarm
}

fun repeatLabel(mode: com.foksi.app.domain.model.RepeatMode): Int = when (mode) {
    com.foksi.app.domain.model.RepeatMode.NONE -> R.string.repeat_none
    com.foksi.app.domain.model.RepeatMode.DAILY -> R.string.repeat_daily
    com.foksi.app.domain.model.RepeatMode.WEEKLY -> R.string.repeat_weekly
    com.foksi.app.domain.model.RepeatMode.MONTHLY -> R.string.repeat_monthly
    com.foksi.app.domain.model.RepeatMode.YEARLY -> R.string.repeat_yearly
    com.foksi.app.domain.model.RepeatMode.WEEKDAYS -> R.string.repeat_weekdays
    com.foksi.app.domain.model.RepeatMode.CUSTOM_DAYS -> R.string.repeat_custom
}
