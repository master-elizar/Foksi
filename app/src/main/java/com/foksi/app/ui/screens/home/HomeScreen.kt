@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.foksi.app.domain.logic.AgendaGroup
import com.foksi.app.domain.logic.PlanFilter
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.notifications.ReminderStatusChecker
import com.foksi.app.ui.components.EmptyState
import com.foksi.app.ui.components.PlanCard
import com.foksi.app.ui.components.ReminderStatusBanner
import com.foksi.app.ui.components.SectionHeader
import com.foksi.app.ui.vm.AgendaViewModel

@Composable
fun HomeScreen(
    viewModel: AgendaViewModel,
    onOpenItem: (Long) -> Unit,
    onAddEvent: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenWhatsNext: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val status = remember(state.settings, state.now) {
        ReminderStatusChecker.check(context, state.settings)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = TimeUtils.formatWeekdayDate(context, state.now),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = greeting(state.now),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.cd_open_search),
                        )
                    }
                }
            }
        }

        item(key = "status") {
            ReminderStatusBanner(
                status = status,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        state.nextUp?.let { next ->
            item(key = "next") {
                NextUpCard(
                    details = next,
                    state = state,
                    onClick = { onOpenItem(next.item.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }

        item(key = "filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filterEntries().forEach { (filter, labelRes) ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(stringResource(labelRes)) },
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }
        }

        item(key = "whatsnext") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_upcoming_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
                TextButton(onClick = onOpenWhatsNext) {
                    Text(stringResource(R.string.nav_whats_next))
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                }
            }
        }

        if (state.loaded && state.sections.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.home_empty_title),
                    subtitle = stringResource(R.string.home_empty_subtitle),
                    icon = Icons.Outlined.EventAvailable,
                    action = {
                        Button(onClick = onAddEvent) {
                            Text(stringResource(R.string.home_empty_action))
                        }
                    },
                )
            }
        }

        state.sections.forEach { section ->
            item(key = "header-${section.group}") {
                SectionHeader(
                    title = groupTitle(section.group),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            items(section.items, key = { "item-${it.item.id}" }) { details ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    PlanCard(
                        details = details,
                        settings = state.settings,
                        now = state.now,
                        onClick = { onOpenItem(details.item.id) },
                        onToggleDone = { viewModel.toggleDone(details.item.id, it) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }

        item(key = "bottom-space") { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
fun NextUpCard(
    details: PlanItemDetails,
    state: com.foksi.app.ui.vm.AgendaUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val start = Agenda.displayStart(details, state.now)
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_next_title),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = details.item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (start != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = TimeUtils.formatDateTime(context, start, state.settings.use24h),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_countdown_label),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = TimeUtils.formatCountdown(context, start, state.now),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onClick) { Text(stringResource(R.string.action_open)) }
                }
            }
            if (details.checklist.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.checklist_progress, details.checklistDone, details.checklistTotal
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun filterEntries(): List<Pair<PlanFilter, Int>> = listOf(
    PlanFilter.ALL to R.string.filter_all,
    PlanFilter.TODAY to R.string.filter_today,
    PlanFilter.TOMORROW to R.string.filter_tomorrow,
    PlanFilter.WEEK to R.string.filter_week,
    PlanFilter.IMPORTANT to R.string.filter_important,
    PlanFilter.OVERDUE to R.string.filter_overdue,
    PlanFilter.WITH_NOTES to R.string.filter_with_notes,
    PlanFilter.COMPLETED to R.string.filter_completed,
)

@Composable
fun groupTitle(group: AgendaGroup): String = stringResource(
    when (group) {
        AgendaGroup.OVERDUE -> R.string.group_overdue
        AgendaGroup.TODAY -> R.string.group_today
        AgendaGroup.TOMORROW -> R.string.group_tomorrow
        AgendaGroup.THIS_WEEK -> R.string.group_this_week
        AgendaGroup.NEXT_WEEK -> R.string.group_next_week
        AgendaGroup.THIS_MONTH -> R.string.group_this_month
        AgendaGroup.LATER -> R.string.group_later
        AgendaGroup.NO_DATE -> R.string.group_no_date
        AgendaGroup.COMPLETED -> R.string.group_completed
    }
)

@Composable
private fun greeting(now: Long): String {
    val hour = TimeUtils.toLocalDateTime(now).hour
    return stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_day
            in 18..22 -> R.string.greeting_evening
            else -> R.string.greeting_night
        }
    )
}
