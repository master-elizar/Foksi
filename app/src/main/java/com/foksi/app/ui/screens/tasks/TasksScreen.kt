package com.foksi.app.ui.screens.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foksi.app.R
import com.foksi.app.domain.model.ItemType
import com.foksi.app.ui.components.EmptyState
import com.foksi.app.ui.components.PlanCard
import com.foksi.app.ui.components.SectionHeader
import com.foksi.app.ui.vm.AgendaViewModel

/** Tasks and notes: the things without a fixed slot in the calendar. */
@Composable
fun TasksScreen(
    viewModel: AgendaViewModel,
    onOpenItem: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val all = state.all
    val tasks = all.filter { it.item.type == ItemType.TASK }
    val notes = all.filter { it.item.type == ItemType.NOTE }
    val openTasks = tasks.filter { !it.item.completed }
    val doneTasks = tasks.filter { it.item.completed }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.tasks_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        item(key = "categories") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.categoryId == null,
                    onClick = { viewModel.setCategory(null) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    shape = RoundedCornerShape(16.dp),
                )
                state.categories.forEach { category ->
                    FilterChip(
                        selected = state.categoryId == category.id,
                        onClick = { viewModel.setCategory(category.id) },
                        label = { Text(categoryLabel(category.builtInKey, category.name)) },
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }
        }

        if (tasks.isEmpty() && notes.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.tasks_empty),
                    subtitle = stringResource(R.string.home_empty_subtitle),
                    icon = Icons.Outlined.TaskAlt,
                )
            }
        }

        if (openTasks.isNotEmpty()) {
            item(key = "open-header") {
                SectionHeader(
                    stringResource(R.string.tasks_open),
                    Modifier.padding(horizontal = 20.dp)
                )
            }
            items(openTasks, key = { "t-${it.item.id}" }) { details ->
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

        if (notes.isNotEmpty()) {
            item(key = "notes-header") {
                SectionHeader(
                    stringResource(R.string.tasks_notes),
                    Modifier.padding(horizontal = 20.dp)
                )
            }
            items(notes, key = { "n-${it.item.id}" }) { details ->
                PlanCard(
                    details = details,
                    settings = state.settings,
                    now = state.now,
                    onClick = { onOpenItem(details.item.id) },
                    onToggleDone = { },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        if (doneTasks.isNotEmpty()) {
            item(key = "done-header") {
                SectionHeader(
                    stringResource(R.string.tasks_done),
                    Modifier.padding(horizontal = 20.dp)
                )
            }
            items(doneTasks, key = { "d-${it.item.id}" }) { details ->
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

        item(key = "space") { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
fun categoryLabel(builtInKey: String?, name: String): String = when (builtInKey) {
    "work" -> stringResource(R.string.category_work)
    "study" -> stringResource(R.string.category_study)
    "personal" -> stringResource(R.string.category_personal)
    "meetings" -> stringResource(R.string.category_meetings)
    "important" -> stringResource(R.string.category_important)
    "other" -> stringResource(R.string.category_other)
    else -> name
}
