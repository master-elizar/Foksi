@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foksi.app.BuildConfig
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.model.PaletteId
import com.foksi.app.domain.model.TextSize
import com.foksi.app.domain.model.ThemeMode
import com.foksi.app.notifications.ReminderStatusChecker
import com.foksi.app.ui.components.FoksiTimePickerDialog
import com.foksi.app.ui.components.ReminderStatusBanner
import com.foksi.app.ui.components.SectionHeader
import com.foksi.app.ui.vm.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var editQuietFrom by remember { mutableStateOf(false) }
    var editQuietTo by remember { mutableStateOf(false) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportJson) }

    val exportIcsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri -> uri?.let(viewModel::exportIcs) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::import) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            val text = if (message.arg != null) {
                context.getString(message.textRes, message.arg)
            } else {
                context.getString(message.textRes)
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.nav_settings),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            item(key = "status") {
                ReminderStatusBanner(
                    status = ReminderStatusChecker.check(context, settings),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item(key = "appearance") {
                SettingsSection(stringResource(R.string.settings_appearance)) {
                    ChipRow(stringResource(R.string.settings_theme)) {
                        themeEntries().forEach { (mode, labelRes) ->
                            FilterChip(
                                selected = settings.theme == mode,
                                onClick = { viewModel.setTheme(mode) },
                                label = { Text(stringResource(labelRes)) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                    ChipRow(stringResource(R.string.settings_palette)) {
                        paletteEntries().forEach { (palette, labelRes) ->
                            FilterChip(
                                selected = settings.palette == palette,
                                onClick = { viewModel.setPalette(palette) },
                                label = { Text(stringResource(labelRes)) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                    ChipRow(stringResource(R.string.settings_text_size)) {
                        textSizeEntries().forEach { (size, labelRes) ->
                            FilterChip(
                                selected = settings.textSize == size,
                                onClick = { viewModel.setTextSize(size) },
                                label = { Text(stringResource(labelRes)) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                }
            }

            item(key = "notifications") {
                SettingsSection(stringResource(R.string.settings_notifications)) {
                    SwitchRow(
                        stringResource(R.string.settings_notifications_enabled),
                        settings.notificationsEnabled,
                        viewModel::setNotificationsEnabled
                    )
                    SwitchRow(
                        stringResource(R.string.settings_normal_enabled),
                        settings.normalEnabled,
                        viewModel::setNormalEnabled
                    )
                    SwitchRow(
                        stringResource(R.string.settings_important_enabled),
                        settings.importantEnabled,
                        viewModel::setImportantEnabled
                    )
                    SwitchRow(
                        stringResource(R.string.settings_alarms_enabled),
                        settings.alarmsEnabled,
                        viewModel::setAlarmsEnabled
                    )
                    SwitchRow(
                        stringResource(R.string.settings_sound),
                        settings.soundEnabled,
                        viewModel::setSoundEnabled
                    )
                    SwitchRow(
                        stringResource(R.string.settings_vibration),
                        settings.vibrationEnabled,
                        viewModel::setVibrationEnabled
                    )
                    SwitchRow(
                        stringResource(R.string.settings_lockscreen),
                        settings.lockScreenEnabled,
                        viewModel::setLockScreenEnabled
                    )
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    ReminderStatusChecker.appNotificationSettings(context)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_notification_sound)) }
                }
            }

            item(key = "quiet") {
                SettingsSection(stringResource(R.string.settings_quiet_hours)) {
                    SwitchRow(
                        stringResource(R.string.settings_quiet_hours),
                        settings.quietHoursEnabled,
                        viewModel::setQuietEnabled
                    )
                    if (settings.quietHoursEnabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_quiet_from),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            AssistChip(
                                onClick = { editQuietFrom = true },
                                label = {
                                    Text(
                                        TimeUtils.formatClock(settings.quietFromMinutes, settings.use24h)
                                    )
                                },
                            )
                            Text(
                                text = stringResource(R.string.settings_quiet_to),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            AssistChip(
                                onClick = { editQuietTo = true },
                                label = {
                                    Text(
                                        TimeUtils.formatClock(settings.quietToMinutes, settings.use24h)
                                    )
                                },
                            )
                        }
                        SwitchRow(
                            stringResource(R.string.settings_quiet_allow_important),
                            settings.quietAllowImportant,
                            viewModel::setQuietAllowImportant
                        )
                        SwitchRow(
                            stringResource(R.string.settings_quiet_allow_alarms),
                            settings.quietAllowAlarms,
                            viewModel::setQuietAllowAlarms
                        )
                    }
                    SwitchRow(
                        stringResource(R.string.settings_advance_default),
                        settings.advanceDefaultEnabled,
                        viewModel::setAdvanceDefault
                    )
                    ChipRow(stringResource(R.string.settings_advance_per_week)) {
                        listOf(0, 1, 2, 3, 5, 7).forEach { value ->
                            FilterChip(
                                selected = settings.advanceDefaultPerWeek == value,
                                onClick = { viewModel.setAdvancePerWeek(value) },
                                label = { Text(value.toString()) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                    ChipRow(stringResource(R.string.settings_min_gap)) {
                        listOf(2, 4, 6, 8, 12, 24).forEach { value ->
                            FilterChip(
                                selected = settings.minGapHours == value,
                                onClick = { viewModel.setMinGapHours(value) },
                                label = { Text(stringResource(R.string.settings_min_gap_value, value)) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                }
            }

            item(key = "calendar") {
                SettingsSection(stringResource(R.string.settings_calendar)) {
                    ChipRow(stringResource(R.string.settings_first_day)) {
                        FilterChip(
                            selected = settings.firstDayOfWeek == 1,
                            onClick = { viewModel.setFirstDayOfWeek(1) },
                            label = {
                                Text(
                                    java.time.DayOfWeek.MONDAY.getDisplayName(
                                        java.time.format.TextStyle.FULL, java.util.Locale.getDefault()
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                        )
                        FilterChip(
                            selected = settings.firstDayOfWeek == 7,
                            onClick = { viewModel.setFirstDayOfWeek(7) },
                            label = {
                                Text(
                                    java.time.DayOfWeek.SUNDAY.getDisplayName(
                                        java.time.format.TextStyle.FULL, java.util.Locale.getDefault()
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                    ChipRow(stringResource(R.string.settings_time_format)) {
                        FilterChip(
                            selected = settings.use24h,
                            onClick = { viewModel.setUse24h(true) },
                            label = { Text(stringResource(R.string.time_format_24)) },
                            shape = RoundedCornerShape(16.dp),
                        )
                        FilterChip(
                            selected = !settings.use24h,
                            onClick = { viewModel.setUse24h(false) },
                            label = { Text(stringResource(R.string.time_format_12)) },
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                    ChipRow(stringResource(R.string.settings_default_duration)) {
                        listOf(15, 30, 45, 60, 90, 120).forEach { value ->
                            FilterChip(
                                selected = settings.defaultDurationMinutes == value,
                                onClick = { viewModel.setDefaultDuration(value) },
                                label = { Text(TimeUtils.formatDuration(context, value)) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                }
            }

            item(key = "widgets") {
                SettingsSection(stringResource(R.string.settings_widgets)) {
                    ChipRow(stringResource(R.string.settings_widget_count)) {
                        listOf(3, 4, 5, 6, 8, 10).forEach { value ->
                            FilterChip(
                                selected = settings.widgetEventCount == value,
                                onClick = { viewModel.setWidgetCount(value) },
                                label = { Text(value.toString()) },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                    SwitchRow(
                        stringResource(R.string.settings_widget_notes),
                        settings.widgetShowNotes,
                        viewModel::setWidgetNotes
                    )
                    SwitchRow(
                        stringResource(R.string.settings_widget_completed),
                        settings.widgetShowCompleted,
                        viewModel::setWidgetCompleted
                    )
                    SwitchRow(
                        stringResource(R.string.settings_widget_countdown),
                        settings.widgetShowCountdown,
                        viewModel::setWidgetCountdown
                    )
                }
            }

            item(key = "data") {
                SettingsSection(stringResource(R.string.settings_data)) {
                    OutlinedButton(
                        onClick = { exportJsonLauncher.launch("foksi-export.json") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_export_json)) }
                    OutlinedButton(
                        onClick = { exportIcsLauncher.launch("foksi-export.ics") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_export_ics)) }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/json", "text/calendar", "text/plain", "*/*")
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_import)) }
                    OutlinedButton(
                        onClick = { viewModel.createBackup() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_backup)) }
                    OutlinedButton(
                        onClick = { viewModel.restoreBackup() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.settings_restore)) }
                }
            }

            item(key = "about") {
                SettingsSection(stringResource(R.string.settings_about)) {
                    Text(
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_about_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    ReminderStatusChecker.batterySettings()
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.status_battery)) }
                }
            }

            item(key = "space") { Spacer(Modifier.height(90.dp)) }
        }
        SnackbarHost(hostState = snackbarHostState)
    }

    if (editQuietFrom) {
        FoksiTimePickerDialog(
            initialMinuteOfDay = settings.quietFromMinutes,
            use24h = settings.use24h,
            onDismiss = { editQuietFrom = false },
            onPicked = { time -> viewModel.setQuietFrom(time.hour * 60 + time.minute) },
        )
    }
    if (editQuietTo) {
        FoksiTimePickerDialog(
            initialMinuteOfDay = settings.quietToMinutes,
            use24h = settings.use24h,
            onDismiss = { editQuietTo = false },
            onPicked = { time -> viewModel.setQuietTo(time.hour * 60 + time.minute) },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        SectionHeader(title)
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
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChipRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

private fun themeEntries(): List<Pair<ThemeMode, Int>> = listOf(
    ThemeMode.SYSTEM to R.string.settings_theme_system,
    ThemeMode.LIGHT to R.string.settings_theme_light,
    ThemeMode.DARK to R.string.settings_theme_dark,
)

private fun paletteEntries(): List<Pair<PaletteId, Int>> = listOf(
    PaletteId.UBUNTU to R.string.palette_ubuntu,
    PaletteId.LAVENDER to R.string.palette_lavender,
    PaletteId.MINT to R.string.palette_mint,
    PaletteId.SKY to R.string.palette_sky,
    PaletteId.ROSE to R.string.palette_rose,
)

private fun textSizeEntries(): List<Pair<TextSize, Int>> = listOf(
    TextSize.SMALL to R.string.text_size_small,
    TextSize.NORMAL to R.string.text_size_normal,
    TextSize.LARGE to R.string.text_size_large,
    TextSize.HUGE to R.string.text_size_huge,
)
