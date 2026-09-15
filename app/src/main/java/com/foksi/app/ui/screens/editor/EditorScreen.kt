@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.screens.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.Attachment
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.Priority
import com.foksi.app.ui.components.FoksiDatePickerDialog
import com.foksi.app.ui.components.FoksiTimePickerDialog
import com.foksi.app.ui.components.SectionHeader
import com.foksi.app.ui.screens.detail.priorityLabel
import com.foksi.app.ui.screens.tasks.categoryLabel
import com.foksi.app.ui.vm.EditorViewModel

private val PRESET_COLORS = listOf(
    0xFFE95420.toInt(), 0xFF7B68EE.toInt(), 0xFF3BA776.toInt(),
    0xFF3A86C8.toInt(), 0xFFD2496B.toInt(), 0xFFC98A2E.toInt(),
    0xFF8A8175.toInt(),
)

private val DURATION_PRESETS = listOf(15, 30, 45, 60, 90, 120, 180, 240, 480)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    eventId: Long,
    initialType: ItemType,
    prefillStart: Long?,
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    LaunchedEffect(eventId, initialType) { viewModel.init(eventId, initialType, prefillStart) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val item = state.details.item
    val isBirthday = item.type == ItemType.BIRTHDAY

    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
            viewModel.addAttachment(
                Attachment(
                    uri = uri.toString(),
                    name = name.ifBlank { uri.toString() },
                    mime = context.contentResolver.getType(uri).orEmpty(),
                    isLink = false,
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle(state.isNew, item.type)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onSaved) }) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 60.dp,
                start = 20.dp,
                end = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isNew) {
                item(key = "type") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        typeEntries().forEach { (type, labelRes) ->
                            FilterChip(
                                selected = item.type == type,
                                onClick = {
                                    viewModel.setType(type)
                                    viewModel.setHasDate(
                                        type == ItemType.EVENT || type == ItemType.BIRTHDAY
                                    )
                                },
                                label = { Text(stringResource(labelRes)) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                }
            }

            item(key = "title") {
                OutlinedTextField(
                    value = item.title,
                    onValueChange = viewModel::setTitle,
                    label = {
                        Text(
                            stringResource(
                                if (isBirthday) R.string.field_person else R.string.field_title
                            )
                        )
                    },
                    placeholder = {
                        Text(
                            stringResource(
                                if (isBirthday) R.string.field_person_hint
                                else R.string.field_title_hint
                            )
                        )
                    },
                    isError = state.errorRes != null,
                    supportingText = { state.errorRes?.let { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "when") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(stringResource(R.string.section_basics))
                    EditorCard {
                        if (isBirthday) {
                            val birth = item.startAt ?: TimeUtils.now()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.field_birth_date),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                AssistChip(
                                    onClick = { showDate = true },
                                    label = {
                                        Text(
                                            if (item.birthYearKnown) {
                                                TimeUtils.formatFullDate(context, birth)
                                            } else {
                                                TimeUtils.formatDate(context, birth)
                                            }
                                        )
                                    },
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.field_birth_year_known),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.field_birth_year_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = item.birthYearKnown,
                                    onCheckedChange = viewModel::setBirthYearKnown,
                                )
                            }
                            return@EditorCard
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.field_has_date),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Switch(checked = state.hasDate, onCheckedChange = viewModel::setHasDate)
                        }
                        if (state.hasDate) {
                            val start = item.startAt ?: TimeUtils.now()
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = { showDate = true },
                                    label = { Text(TimeUtils.formatDate(context, start)) },
                                )
                                if (!item.allDay) {
                                    AssistChip(
                                        onClick = { showTime = true },
                                        label = {
                                            Text(
                                                TimeUtils.formatTime(context, start, state.settings.use24h)
                                            )
                                        },
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.field_all_day),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Switch(checked = item.allDay, onCheckedChange = viewModel::setAllDay)
                            }
                            if (!item.allDay) {
                                Text(
                                    text = stringResource(R.string.field_duration) + ": " +
                                        TimeUtils.formatDuration(context, item.durationMinutes),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    DURATION_PRESETS.forEach { minutes ->
                                        FilterChip(
                                            selected = item.durationMinutes == minutes,
                                            onClick = { viewModel.setDuration(minutes) },
                                            label = { Text(TimeUtils.formatDuration(context, minutes)) },
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "meta") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(stringResource(R.string.section_details))
                    EditorCard {
                        Text(
                            text = stringResource(R.string.field_priority),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Priority.entries.forEach { priority ->
                                FilterChip(
                                    selected = item.priority == priority,
                                    onClick = { viewModel.setPriority(priority) },
                                    label = { Text(stringResource(priorityLabel(priority))) },
                                    shape = RoundedCornerShape(16.dp),
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.field_category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = item.categoryId == null,
                                onClick = { viewModel.setCategory(null) },
                                label = { Text(stringResource(R.string.category_none)) },
                                shape = RoundedCornerShape(16.dp),
                            )
                            state.categories.forEach { category ->
                                FilterChip(
                                    selected = item.categoryId == category.id,
                                    onClick = { viewModel.setCategory(category.id) },
                                    label = { Text(categoryLabel(category.builtInKey, category.name)) },
                                    shape = RoundedCornerShape(16.dp),
                                )
                            }
                            AssistChip(
                                onClick = { showCategoryDialog = true },
                                label = { Text(stringResource(R.string.category_new)) },
                            )
                        }

                        Text(
                            text = stringResource(R.string.field_color),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PRESET_COLORS.forEach { argb ->
                                Box(
                                    modifier = Modifier
                                        .size(if (item.colorArgb == argb) 30.dp else 26.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .clickable {
                                            viewModel.setColor(if (item.colorArgb == argb) null else argb)
                                        }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "text") {
                EditorCard {
                    if (!isBirthday) {
                    OutlinedTextField(
                        value = item.location,
                        onValueChange = viewModel::setLocation,
                        label = { Text(stringResource(R.string.field_location)) },
                        placeholder = { Text(stringResource(R.string.field_location_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.url,
                        onValueChange = viewModel::setUrl,
                        label = { Text(stringResource(R.string.field_url)) },
                        placeholder = { Text(stringResource(R.string.field_url_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.description,
                        onValueChange = viewModel::setDescription,
                        label = { Text(stringResource(R.string.field_description)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    }
                    OutlinedTextField(
                        value = item.notes,
                        onValueChange = viewModel::setNotes,
                        label = { Text(stringResource(R.string.field_notes)) },
                        placeholder = { Text(stringResource(R.string.field_notes_hint)) },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.hasDate) {
                item(key = "reminders") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RemindersSection(
                            reminders = state.details.reminders,
                            onAdd = viewModel::addReminder,
                            onRemove = viewModel::removeReminder,
                            onUpdate = viewModel::updateReminder,
                        )
                    }
                }
                if (!isBirthday) {
                item(key = "advance") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AdvanceSection(
                            advance = item.advance,
                            use24h = state.settings.use24h,
                            onChange = { transform -> viewModel.updateAdvance(transform) },
                        )
                    }
                }
                item(key = "repeat") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RepeatSection(rule = item.repeat, onChange = viewModel::setRepeat)
                    }
                }
                }
            }

            item(key = "checklist") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChecklistSection(
                        items = state.details.checklist,
                        onAdd = viewModel::addChecklistItem,
                        onToggle = viewModel::toggleChecklistItem,
                        onEdit = viewModel::setChecklistText,
                        onRemove = viewModel::removeChecklistItem,
                        onMove = viewModel::moveChecklistItem,
                    )
                }
            }

            item(key = "attachments") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader(stringResource(R.string.section_attachments))
                    EditorCard {
                        state.details.attachments.forEachIndexed { index, attachment ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (attachment.isLink) Icons.Outlined.Link else Icons.Outlined.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = attachment.name.ifBlank { attachment.uri },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.removeAttachment(index) }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.cd_remove),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                                Text(stringResource(R.string.attachment_add_file))
                            }
                            OutlinedButton(onClick = { showLinkDialog = true }) {
                                Text(stringResource(R.string.attachment_add_link))
                            }
                        }
                    }
                }
            }

            item(key = "save") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.save(onSaved) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) { Text(stringResource(R.string.action_save)) }
                }
            }
        }
    }

    if (showDate) {
        FoksiDatePickerDialog(
            initialMillis = item.startAt ?: TimeUtils.now(),
            onDismiss = { showDate = false },
            onPicked = viewModel::setDate,
        )
    }
    if (showTime) {
        FoksiTimePickerDialog(
            initialMinuteOfDay = TimeUtils.minutesOfDay(item.startAt ?: TimeUtils.now()),
            use24h = state.settings.use24h,
            onDismiss = { showTime = false },
            onPicked = viewModel::setTime,
        )
    }
    if (showLinkDialog) {
        TextInputDialog(
            titleRes = R.string.attachment_add_link,
            placeholderRes = R.string.attachment_link_hint,
            onDismiss = { showLinkDialog = false },
            onConfirm = { value ->
                viewModel.addAttachment(Attachment(uri = value, name = value, isLink = true))
                showLinkDialog = false
            }
        )
    }
    if (showCategoryDialog) {
        TextInputDialog(
            titleRes = R.string.category_new,
            placeholderRes = R.string.category_name,
            onDismiss = { showCategoryDialog = false },
            onConfirm = { value ->
                viewModel.addCategory(value, PRESET_COLORS.random())
                showCategoryDialog = false
            }
        )
    }
}

@Composable
private fun TextInputDialog(
    titleRes: Int,
    placeholderRes: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(stringResource(placeholderRes)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun screenTitle(isNew: Boolean, type: ItemType): String = stringResource(
    when {
        isNew && type == ItemType.BIRTHDAY -> R.string.editor_new_birthday
        !isNew && type == ItemType.BIRTHDAY -> R.string.editor_edit_birthday
        isNew && type == ItemType.TASK -> R.string.editor_new_task
        isNew && type == ItemType.NOTE -> R.string.editor_new_note
        isNew -> R.string.editor_new_event
        type == ItemType.TASK -> R.string.editor_edit_task
        type == ItemType.NOTE -> R.string.editor_edit_note
        else -> R.string.editor_edit_event
    }
)

private fun typeEntries(): List<Pair<ItemType, Int>> = listOf(
    ItemType.EVENT to R.string.type_event,
    ItemType.TASK to R.string.type_task,
    ItemType.NOTE to R.string.type_note,
    ItemType.BIRTHDAY to R.string.type_birthday,
)
