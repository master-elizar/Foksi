@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.screens.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.AdvanceConfig
import com.foksi.app.domain.model.AdvanceMode
import com.foksi.app.domain.model.ChecklistItem
import com.foksi.app.domain.model.Reminder
import com.foksi.app.domain.model.ReminderType
import com.foksi.app.domain.model.RepeatMode
import com.foksi.app.domain.model.RepeatRule
import com.foksi.app.ui.components.FoksiTimePickerDialog
import com.foksi.app.ui.components.SectionHeader
import com.foksi.app.ui.screens.detail.reminderTypeLabel

/** Offsets offered when adding a reminder, expressed in minutes before the event. */
val REMINDER_PRESETS = listOf(0, 5, 10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 4320, 10080, 20160)

@Composable
fun EditorCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun RemindersSection(
    reminders: List<Reminder>,
    onAdd: (Int, ReminderType) -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, (Reminder) -> Reminder) -> Unit,
) {
    val context = LocalContext.current
    var showPresets by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var customValue by remember { mutableStateOf("60") }

    SectionHeader(stringResource(R.string.section_reminders))
    EditorCard {
        if (reminders.isEmpty()) {
            Text(
                text = stringResource(R.string.reminder_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        reminders.forEachIndexed { index, reminder ->
            var typeMenu by remember(index) { mutableStateOf(false) }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (reminder.minutesBefore <= 0) {
                            stringResource(R.string.reminder_at_time)
                        } else {
                            stringResource(
                                R.string.reminder_before,
                                TimeUtils.formatOffset(context, reminder.minutesBefore)
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        TextButton(onClick = { typeMenu = true }) {
                            Text(stringResource(reminderTypeLabel(reminder.type)))
                        }
                        DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                            ReminderType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(reminderTypeLabel(type))) },
                                    onClick = {
                                        typeMenu = false
                                        onUpdate(index) { it.copy(type = type) }
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.cd_remove),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (reminder.type != ReminderType.NORMAL) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = reminder.repeatUntilAck,
                            onCheckedChange = { value ->
                                onUpdate(index) { it.copy(repeatUntilAck = value) }
                            }
                        )
                        Text(
                            text = stringResource(R.string.reminder_repeat_until_ack),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (reminder.repeatUntilAck) {
                        Text(
                            text = stringResource(
                                R.string.reminder_repeat_every, reminder.repeatIntervalMinutes
                            ) + " · " + stringResource(
                                R.string.reminder_repeat_times, reminder.repeatMaxCount
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = reminder.repeatMaxCount.toFloat(),
                            onValueChange = { value ->
                                onUpdate(index) { it.copy(repeatMaxCount = value.toInt().coerceIn(1, 10)) }
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                        )
                    }
                }
            }
        }

        Box {
            TextButton(onClick = { showPresets = true }) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.reminder_add))
            }
            DropdownMenu(expanded = showPresets, onDismissRequest = { showPresets = false }) {
                REMINDER_PRESETS.forEach { minutes ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (minutes == 0) stringResource(R.string.reminder_at_time)
                                else stringResource(
                                    R.string.reminder_before,
                                    TimeUtils.formatOffset(context, minutes)
                                )
                            )
                        },
                        onClick = {
                            showPresets = false
                            onAdd(minutes, ReminderType.NORMAL)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reminder_custom)) },
                    onClick = {
                        showPresets = false
                        showCustom = true
                    }
                )
            }
        }
    }

    if (showCustom) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text(stringResource(R.string.custom_minutes_before)) },
            text = {
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { input -> customValue = input.filter { it.isDigit() }.take(6) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    customValue.toIntOrNull()?.let { onAdd(it, ReminderType.NORMAL) }
                    showCustom = false
                }) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustom = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun AdvanceSection(
    advance: AdvanceConfig,
    use24h: Boolean,
    onChange: ((AdvanceConfig) -> AdvanceConfig) -> Unit,
) {
    val context = LocalContext.current
    var editWindowStart by remember { mutableStateOf(false) }
    var editWindowEnd by remember { mutableStateOf(false) }

    SectionHeader(stringResource(R.string.section_advance))
    EditorCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.advance_enable),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.advance_explainer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = advance.enabled,
                onCheckedChange = { value -> onChange { it.copy(enabled = value) } },
            )
        }

        if (!advance.enabled) return@EditorCard

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            advanceModes().forEach { (mode, labelRes) ->
                FilterChip(
                    selected = advance.mode == mode,
                    onClick = { onChange { it.copy(mode = mode) } },
                    label = { Text(stringResource(labelRes)) },
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }

        Text(
            text = stringResource(R.string.advance_start) + ": " +
                context.resources.getQuantityString(
                    R.plurals.days_short, advance.startDaysBefore, advance.startDaysBefore
                ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = advance.startDaysBefore.toFloat(),
            onValueChange = { value -> onChange { it.copy(startDaysBefore = value.toInt().coerceIn(1, 90)) } },
            valueRange = 1f..90f,
        )

        when (advance.mode) {
            AdvanceMode.CHAIN -> {
                Text(
                    text = stringResource(R.string.advance_chain_editable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AdvanceConfig.CHAIN_STEPS
                        .filter { it <= advance.startDaysBefore * 24 * 60 }
                        .forEach { step ->
                            val enabled = step !in advance.disabledChainSteps
                            FilterChip(
                                selected = enabled,
                                onClick = {
                                    onChange { current ->
                                        val steps = current.disabledChainSteps.toMutableSet()
                                        if (enabled) steps += step else steps -= step
                                        current.copy(disabledChainSteps = steps)
                                    }
                                },
                                label = { Text(TimeUtils.formatOffset(context, step)) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                }
            }

            AdvanceMode.INTERVAL -> {
                Text(
                    text = stringResource(R.string.advance_interval_days) + ": ${advance.intervalDays}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = advance.intervalDays.toFloat(),
                    onValueChange = { value ->
                        onChange { it.copy(intervalDays = value.toInt().coerceIn(1, 30)) }
                    },
                    valueRange = 1f..30f,
                )
            }

            AdvanceMode.RANDOM -> {
                Text(
                    text = stringResource(R.string.advance_per_week) + ": ${advance.perWeek}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = advance.perWeek.toFloat(),
                    onValueChange = { value ->
                        onChange { it.copy(perWeek = value.toInt().coerceIn(0, 7)) }
                    },
                    valueRange = 0f..7f,
                    steps = 6,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.advance_window),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            AssistChip(
                onClick = { editWindowStart = true },
                label = { Text(TimeUtils.formatClock(advance.windowStartMinutes, use24h)) },
            )
            Spacer(Modifier.size(6.dp))
            AssistChip(
                onClick = { editWindowEnd = true },
                label = { Text(TimeUtils.formatClock(advance.windowEndMinutes, use24h)) },
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.advance_workdays_only),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = advance.workdaysOnly,
                onCheckedChange = { value -> onChange { it.copy(workdaysOnly = value) } },
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.advance_checklist_nudge),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = advance.checklistNudge,
                onCheckedChange = { value -> onChange { it.copy(checklistNudge = value) } },
            )
        }
    }

    if (editWindowStart) {
        FoksiTimePickerDialog(
            initialMinuteOfDay = advance.windowStartMinutes,
            use24h = use24h,
            onDismiss = { editWindowStart = false },
            onPicked = { time ->
                onChange { it.copy(windowStartMinutes = time.hour * 60 + time.minute) }
            }
        )
    }
    if (editWindowEnd) {
        FoksiTimePickerDialog(
            initialMinuteOfDay = advance.windowEndMinutes,
            use24h = use24h,
            onDismiss = { editWindowEnd = false },
            onPicked = { time ->
                onChange { it.copy(windowEndMinutes = time.hour * 60 + time.minute) }
            }
        )
    }
}

@Composable
fun RepeatSection(rule: RepeatRule, onChange: (RepeatRule) -> Unit) {
    SectionHeader(stringResource(R.string.section_repeat))
    EditorCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeatModes().forEach { (mode, labelRes) ->
                FilterChip(
                    selected = rule.mode == mode,
                    onClick = { onChange(rule.copy(mode = mode)) },
                    label = { Text(stringResource(labelRes)) },
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }
        if (rule.mode == RepeatMode.CUSTOM_DAYS || rule.mode == RepeatMode.WEEKLY ||
            rule.mode == RepeatMode.MONTHLY || rule.mode == RepeatMode.YEARLY
        ) {
            Text(
                text = stringResource(R.string.repeat_interval) + ": ${rule.interval}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = rule.interval.toFloat(),
                onValueChange = { onChange(rule.copy(interval = it.toInt().coerceIn(1, 30))) },
                valueRange = 1f..30f,
            )
        }
        if (rule.mode == RepeatMode.WEEKLY) {
            Text(
                text = stringResource(R.string.repeat_weekdays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (1..7).forEach { day ->
                    val selected = rule.weekDays.contains(day)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val days = rule.weekDays.toMutableSet()
                            if (selected) days -= day else days += day
                            onChange(rule.copy(weekDays = days))
                        },
                        label = {
                            Text(
                                java.time.DayOfWeek.of(day)
                                    .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ChecklistSection(
    items: List<ChecklistItem>,
    onAdd: (String) -> Unit,
    onToggle: (Int) -> Unit,
    onEdit: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    var newItem by remember { mutableStateOf("") }

    SectionHeader(
        if (items.isEmpty()) {
            stringResource(R.string.section_checklist)
        } else {
            stringResource(R.string.checklist_progress, items.count { it.done }, items.size)
        }
    )
    EditorCard {
        items.forEachIndexed { index, entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = entry.done, onCheckedChange = { onToggle(index) })
                OutlinedTextField(
                    value = entry.text,
                    onValueChange = { onEdit(index, it) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onMove(index, -1) }, enabled = index > 0) {
                    Icon(
                        Icons.Outlined.ArrowUpward,
                        contentDescription = stringResource(R.string.checklist_move_up),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = { onMove(index, 1) }, enabled = index < items.lastIndex) {
                    Icon(
                        Icons.Outlined.ArrowDownward,
                        contentDescription = stringResource(R.string.checklist_move_down),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = { onRemove(index) }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_remove),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                placeholder = { Text(stringResource(R.string.checklist_hint)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onAdd(newItem)
                    newItem = ""
                },
                enabled = newItem.isNotBlank(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.checklist_add))
            }
        }
    }
}

private fun advanceModes(): List<Pair<AdvanceMode, Int>> = listOf(
    AdvanceMode.CHAIN to R.string.advance_mode_chain,
    AdvanceMode.INTERVAL to R.string.advance_mode_interval,
    AdvanceMode.RANDOM to R.string.advance_mode_random,
)

private fun repeatModes(): List<Pair<RepeatMode, Int>> = listOf(
    RepeatMode.NONE to R.string.repeat_none,
    RepeatMode.DAILY to R.string.repeat_daily,
    RepeatMode.WEEKLY to R.string.repeat_weekly,
    RepeatMode.WEEKDAYS to R.string.repeat_weekdays,
    RepeatMode.MONTHLY to R.string.repeat_monthly,
    RepeatMode.YEARLY to R.string.repeat_yearly,
    RepeatMode.CUSTOM_DAYS to R.string.repeat_custom,
)
