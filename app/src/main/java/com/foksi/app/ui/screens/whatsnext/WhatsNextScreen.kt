package com.foksi.app.ui.screens.whatsnext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foksi.app.R
import com.foksi.app.domain.logic.AgendaGroup
import com.foksi.app.ui.components.EmptyState
import com.foksi.app.ui.components.PlanCard
import com.foksi.app.ui.components.SectionHeader
import com.foksi.app.ui.screens.home.groupTitle
import com.foksi.app.ui.vm.AgendaViewModel

/** A strictly chronological view: today, tomorrow, this week, later — with a countdown each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNextScreen(
    viewModel: AgendaViewModel,
    onOpenItem: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_whats_next)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val sections = state.sections.filter { it.group != AgendaGroup.COMPLETED }
        if (sections.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                subtitle = stringResource(R.string.home_no_upcoming),
                icon = Icons.Outlined.EventAvailable,
            )
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            sections.forEach { section ->
                item(key = "h-${section.group}") {
                    SectionHeader(
                        title = groupTitle(section.group),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                items(section.items, key = { "wn-${it.item.id}" }) { details ->
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
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
