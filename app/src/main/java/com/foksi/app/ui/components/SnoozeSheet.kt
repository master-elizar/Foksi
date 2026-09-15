package com.foksi.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.foksi.app.R
import com.foksi.app.notifications.SnoozeOption

@Composable
fun SnoozeOptionsDialog(
    onDismiss: () -> Unit,
    onPicked: (SnoozeOption) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.snooze_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SnoozeOption.entries.forEach { option ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPicked(option) }
                    ) {
                        Text(
                            text = stringResource(option.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    )
}
