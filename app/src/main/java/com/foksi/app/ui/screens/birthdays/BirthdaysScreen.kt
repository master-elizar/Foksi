@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.screens.birthdays

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.domain.logic.Birthdays
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.ui.components.EmptyState
import com.foksi.app.ui.components.SectionHeader
import com.foksi.app.ui.vm.BirthdayViewModel

/** Avatar palette — muted, in the same family as the app's accent colours. */
private val AVATAR_COLORS = listOf(
    0xFFE9A98B, 0xFFB7A6E0, 0xFF8FC7AE, 0xFF8FB8D8, 0xFFE0A0B4, 0xFFD9C08A,
)

@Composable
fun BirthdaysScreen(
    viewModel: BirthdayViewModel,
    onOpenItem: (Long) -> Unit,
    onAddBirthday: () -> Unit,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item(key = "title") {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(
                    text = stringResource(R.string.birthdays_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.total > 0) {
                    Text(
                        text = stringResource(R.string.birthdays_count, state.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val nearest = state.nearest
        if (nearest != null) {
            item(key = "hero") {
                NearestBirthdayCard(
                    details = nearest,
                    now = state.now,
                    onClick = { onOpenItem(nearest.item.id) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        if (state.loaded && state.total == 0) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.birthdays_empty_title),
                    subtitle = stringResource(R.string.birthdays_empty_subtitle),
                    icon = Icons.Outlined.Cake,
                    action = {
                        Button(onClick = onAddBirthday) {
                            Text(stringResource(R.string.birthdays_add))
                        }
                    },
                )
            }
        }

        birthdaySection(R.string.birthdays_today, state.today, state, onOpenItem)
        birthdaySection(R.string.birthdays_this_month, state.thisMonth, state, onOpenItem)
        birthdaySection(R.string.birthdays_later, state.later, state, onOpenItem)

        item(key = "space") { Spacer(Modifier.height(90.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.birthdaySection(
    titleRes: Int,
    items: List<PlanItemDetails>,
    state: com.foksi.app.ui.vm.BirthdayUiState,
    onOpenItem: (Long) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "h-$titleRes") {
        SectionHeader(
            title = androidx.compose.ui.res.stringResource(titleRes),
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
    items(items, key = { "b-${it.item.id}" }) { details ->
        BirthdayRow(
            details = details,
            now = state.now,
            onClick = { onOpenItem(details.item.id) },
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun NearestBirthdayCard(
    details: PlanItemDetails,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val item = details.item
    val next = Birthdays.nextOccurrence(item, now)
    val age = Birthdays.ageTurning(item, now)
    val days = Birthdays.daysUntil(item, now)

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = item.title, size = 52)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.birthdays_nearest),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (next != null) append(TimeUtils.formatDate(context, next))
                        if (age != null) {
                            append(" · ")
                            append(context.getString(R.string.birthdays_turns, age))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (days == 0L) {
                        stringResource(R.string.birthdays_today_label)
                    } else {
                        TimeUtils.formatCountdown(context, next ?: now, now)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun BirthdayRow(
    details: PlanItemDetails,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val item = details.item
    val next = Birthdays.nextOccurrence(item, now)
    val age = Birthdays.ageTurning(item, now)
    val days = Birthdays.daysUntil(item, now)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = item.title, size = 40)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        if (next != null) append(TimeUtils.formatDate(context, next))
                        if (age != null) {
                            append(" · ")
                            append(context.resources.getQuantityString(R.plurals.birthdays_age, age, age))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (days == 0L) {
                    stringResource(R.string.birthdays_today_short)
                } else {
                    TimeUtils.formatCountdownShort(context, next ?: now, now)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Avatar(name: String, size: Int) {
    val letter = name.trim().firstOrNull()?.uppercase() ?: "?"
    val color = Color(AVATAR_COLORS[(name.hashCode().mod(AVATAR_COLORS.size))])
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = if (size >= 48) MaterialTheme.typography.titleLarge
            else MaterialTheme.typography.titleMedium,
            color = Color(0xFF2A211C),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
