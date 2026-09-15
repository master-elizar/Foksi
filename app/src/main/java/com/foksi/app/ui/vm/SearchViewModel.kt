package com.foksi.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.PlanItemDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<PlanItemDetails> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel : ViewModel() {

    private val repository = AppGraph.planRepository
    private val interactor = AppGraph.planInteractor
    private val query = MutableStateFlow("")

    val state: StateFlow<SearchUiState> = combine(
        query.asStateFlow(),
        query.debounce(200).flatMapLatest { text ->
            if (text.isBlank()) flowOf(emptyList()) else repository.search(text.trim())
        },
        AppGraph.settingsRepository.settings,
    ) { text, results, settings ->
        SearchUiState(text, results, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun toggleDone(id: Long, done: Boolean) {
        viewModelScope.launch { interactor.setCompleted(id, done) }
    }
}
