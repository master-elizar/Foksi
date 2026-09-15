@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.logic.Agenda
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Priority
import com.foksi.app.ui.theme.LocalFoksiColors

@Composable
fun priorityColor(priority: Priority): Color {
    val extras = LocalFoksiColors.current
    return when (priority) {
        Priority.NORMAL -> MaterialTheme.colorScheme.outline
        Priority.IMPORTANT -> extras.warning
        Priority.CRITICAL -> extras.danger
    }
}

@Composable
fun PlanCard(
    details: PlanItemDetails,
    settings: AppSettings,
    now: Long,
    onClick: () -> Unit,
    onToggleDone: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val item = details.item
    val start = Agenda.displayStart(details, now)
    val accent = item.colorArgb?.let { Color(it) } ?: priorityColor(item.priority)
    val container by animateColorAsState(
        targetValue = if (item.completed) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface,
        label = "cardColor"
    )

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accent,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .width(4.dp)
                    .height(if (item.priority == Priority.CRITICAL) 44.dp else 34.dp)
                    .semantics { contentDescription = "" }
            ) {}

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { context.getString(R.string.type_note) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.priority == Priority.CRITICAL) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                    color = if (item.completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = buildSubtitle(details, settings, start)
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (details.reminders.any { it.enabled }) {
                        BadgeIcon(Icons.Outlined.NotificationsActive)
                    }
                    if (item.repeat.isRepeating) BadgeIcon(Icons.Outlined.Repeat)
                    if (details.checklist.isNotEmpty()) {
                        BadgeIcon(Icons.Outlined.Checklist)
                        Text(
                            text = "${details.checklistDone}/${details.checklistTotal}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (item.location.isNotBlank()) BadgeIcon(Icons.Outlined.Place)
                    if (item.url.isNotBlank()) BadgeIcon(Icons.Outlined.Link)
                }
            }

            Spacer(Modifier.width(8.dp))

            if (item.type == ItemType.EVENT && start != null && !item.completed) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = TimeUtils.formatCountdown(context, start, now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(96.dp),
                    )
                }
            } else if (item.type != ItemType.NOTE) {
                Checkbox(
                    checked = item.completed,
                    onCheckedChange = onToggleDone,
                    modifier = Modifier.semantics {
                        contentDescription = context.getString(R.string.cd_toggle_done)
                    }
                )
            }
        }
    }
}

@Composable
private fun BadgeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(14.dp),
    )
}

@Composable
private fun buildSubtitle(
    details: PlanItemDetails,
    settings: AppSettings,
    start: Long?,
): String {
    val context = LocalContext.current
    val item = details.item
    return when {
        item.type == ItemType.NOTE -> item.notes.lineSequence().firstOrNull().orEmpty()
        start == null -> item.description.lineSequence().firstOrNull().orEmpty()
        item.allDay -> TimeUtils.formatDate(context, start)
        else -> buildString {
            append(TimeUtils.formatDateTime(context, start, settings.use24h))
            if (item.location.isNotBlank()) append(" · ${item.location}")
        }
    }
}
