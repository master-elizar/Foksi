package com.foksi.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoksiDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    // The Material date picker works in UTC, so convert both ways around the local calendar date.
    val localDate = TimeUtils.toLocalDate(initialMillis)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selected = state.selectedDateMillis
                if (selected != null) {
                    onPicked(
                        java.time.Instant.ofEpochMilli(selected).atZone(ZoneOffset.UTC).toLocalDate()
                    )
                }
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoksiTimePickerDialog(
    initialMinuteOfDay: Int,
    use24h: Boolean,
    onDismiss: () -> Unit,
    onPicked: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinuteOfDay / 60).coerceIn(0, 23),
        initialMinute = (initialMinuteOfDay % 60).coerceIn(0, 59),
        is24Hour = use24h,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPicked(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        text = { TimePicker(state = state) }
    )
}
