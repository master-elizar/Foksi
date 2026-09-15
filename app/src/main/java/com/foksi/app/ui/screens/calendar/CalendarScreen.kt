@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.ui.components.EmptyState
import com.foksi.app.ui.components.PlanCard
import com.foksi.app.ui.vm.CalendarUiState
import com.foksi.app.ui.vm.CalendarViewMode
import com.foksi.app.ui.vm.CalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onOpenItem: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item(key = "calendar-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.showMonth(state.month.minusMonths(1)) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBackIos,
                        contentDescription = stringResource(R.string.calendar_prev),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = TimeUtils.formatMonthYear(context, state.month.atDay(1)),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.showMonth(state.month.plusMonths(1)) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = stringResource(R.string.calendar_next),
                        modifier = Modifier.size(16.dp),
                    )
                }
                TextButton(onClick = { viewModel.today() }) {
                    Text(stringResource(R.string.calendar_today))
                }
            }
        }

        item(key = "modes") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                modeEntries().forEach { (mode, labelRes) ->
                    val selected = state.viewMode == mode
                    Surface(
                        onClick = { viewModel.setViewMode(mode) },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
            }
        }

        item(key = "grid") {
            AnimatedContent(
                targetState = state.viewMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "calendarMode",
            ) { mode ->
                when (mode) {
                    CalendarViewMode.MONTH -> MonthGrid(state, viewModel::selectDate)
                    CalendarViewMode.WEEK -> WeekStrip(state, viewModel::selectDate)
                    CalendarViewMode.DAY -> Spacer(Modifier.height(4.dp))
                    CalendarViewMode.AGENDA -> Spacer(Modifier.height(4.dp))
                }
            }
        }

        if (state.viewMode == CalendarViewMode.AGENDA) {
            val dates = state.entriesByDate.keys.filter { !it.isBefore(LocalDate.now()) }.sorted()
            if (dates.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        subtitle = stringResource(R.string.home_no_upcoming),
                    )
                }
            }
            dates.forEach { date ->
                item(key = "agenda-$date") {
                    Text(
                        text = TimeUtils.formatWeekdayDate(context, TimeUtils.startOfDay(date)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    )
                }
                items(
                    state.entriesByDate[date].orEmpty(),
                    key = { "agenda-$date-${it.details.item.id}-${it.start}" },
                ) { entry ->
                    PlanCard(
                        details = entry.details,
                        settings = state.settings,
                        now = TimeUtils.now(),
                        onClick = { onOpenItem(entry.details.item.id) },
                        onToggleDone = { viewModel.toggleDone(entry.details.item.id, it) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        } else {
            val entries = state.entriesByDate[state.selectedDate].orEmpty()
            item(key = "selected-date") {
                Text(
                    text = TimeUtils.formatWeekdayDate(
                        context, TimeUtils.startOfDay(state.selectedDate)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            if (entries.isEmpty()) {
                item(key = "no-events") {
                    Text(
                        text = stringResource(R.string.calendar_no_events),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
            items(entries, key = { "day-${it.details.item.id}-${it.start}" }) { entry ->
                PlanCard(
                    details = entry.details,
                    settings = state.settings,
                    now = TimeUtils.now(),
                    onClick = { onOpenItem(entry.details.item.id) },
                    onToggleDone = { viewModel.toggleDone(entry.details.item.id, it) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        item(key = "space") { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val firstDay = TimeUtils.firstDayOfWeek(state.settings.firstDayOfWeek)
    val weekDays = (0..6).map { firstDay.plus(it.toLong()) }
    val monthStart = state.month.atDay(1)
    val lead = ((monthStart.dayOfWeek.value - firstDay.value) + 7) % 7
    val gridStart = monthStart.minusDays(lead.toLong())
    val totalCells = 42

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        for (week in 0 until totalCells / 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayIndex in 0 until 7) {
                    val date = gridStart.plusDays((week * 7 + dayIndex).toLong())
                    DayCell(
                        date = date,
                        inMonth = date.month == state.month.month,
                        selected = date == state.selectedDate,
                        count = state.entriesByDate[date]?.size ?: 0,
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val firstDay: DayOfWeek = TimeUtils.firstDayOfWeek(state.settings.firstDayOfWeek)
    val offset = ((state.selectedDate.dayOfWeek.value - firstDay.value) + 7) % 7
    val weekStart = state.selectedDate.minusDays(offset.toLong())
    Row(modifier = Modifier.padding(horizontal = 12.dp)) {
        (0..6).forEach { index ->
            val date = weekStart.plusDays(index.toLong())
            DayCell(
                date = date,
                inMonth = true,
                selected = date == state.selectedDate,
                count = state.entriesByDate[date]?.size ?: 0,
                onClick = { onSelect(date) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isToday = date == LocalDate.now()
    Box(
        modifier = modifier
            .aspectRatio(0.95f)
            .padding(3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || selected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    !inMonth -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(count.coerceAtMost(3)) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}

private fun modeEntries(): List<Pair<CalendarViewMode, Int>> = listOf(
    CalendarViewMode.MONTH to R.string.calendar_month,
    CalendarViewMode.WEEK to R.string.calendar_week,
    CalendarViewMode.DAY to R.string.calendar_day,
    CalendarViewMode.AGENDA to R.string.calendar_agenda,
)
