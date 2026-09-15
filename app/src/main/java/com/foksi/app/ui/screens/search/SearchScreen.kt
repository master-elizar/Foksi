package com.foksi.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.ui.components.EmptyState
import com.foksi.app.ui.components.PlanCard
import com.foksi.app.ui.vm.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenItem: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                },
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
        when {
            state.query.isBlank() -> EmptyState(
                title = stringResource(R.string.action_search),
                subtitle = stringResource(R.string.search_start),
                icon = Icons.Outlined.Search,
            )

            state.results.isEmpty() -> EmptyState(
                title = stringResource(R.string.search_empty),
                subtitle = stringResource(R.string.search_hint),
                icon = Icons.Outlined.Search,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.results, key = { "s-${it.item.id}" }) { details ->
                    PlanCard(
                        details = details,
                        settings = state.settings,
                        now = TimeUtils.now(),
                        onClick = { onOpenItem(details.item.id) },
                        onToggleDone = { viewModel.toggleDone(details.item.id, it) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }
    }
}
