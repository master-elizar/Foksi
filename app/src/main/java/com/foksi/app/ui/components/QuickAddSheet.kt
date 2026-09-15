@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.foksi.app.R
import com.foksi.app.domain.model.ItemType

private enum class QuickAddMode { MENU, NOTE, TASK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onCreateEvent: () -> Unit,
    onQuickSave: (ItemType, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(QuickAddMode.MENU) }
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_add_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when (mode) {
                QuickAddMode.MENU -> {
                    QuickAddRow(
                        icon = Icons.Outlined.CalendarMonth,
                        title = stringResource(R.string.quick_add_event),
                        subtitle = stringResource(R.string.quick_add_event_desc),
                        onClick = onCreateEvent,
                    )
                    QuickAddRow(
                        icon = Icons.Outlined.CheckCircle,
                        title = stringResource(R.string.quick_add_task),
                        subtitle = stringResource(R.string.quick_add_task_desc),
                        onClick = { mode = QuickAddMode.TASK },
                    )
                    QuickAddRow(
                        icon = Icons.Outlined.EditNote,
                        title = stringResource(R.string.quick_add_note),
                        subtitle = stringResource(R.string.quick_add_note_desc),
                        onClick = { mode = QuickAddMode.NOTE },
                    )
                }

                else -> {
                    val isNote = mode == QuickAddMode.NOTE
                    LaunchedEffect(mode) { focusRequester.requestFocus() }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = {
                            Text(
                                stringResource(
                                    if (isNote) R.string.quick_note_hint else R.string.quick_task_hint
                                )
                            )
                        },
                        singleLine = !isNote,
                        minLines = if (isNote) 3 else 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (text.isNotBlank()) {
                                onQuickSave(if (isNote) ItemType.NOTE else ItemType.TASK, text)
                            }
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onQuickSave(if (isNote) ItemType.NOTE else ItemType.TASK, text)
                            }
                        },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.action_save)) }
                }
            }
        }
    }
}

@Composable
private fun QuickAddRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.size(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
