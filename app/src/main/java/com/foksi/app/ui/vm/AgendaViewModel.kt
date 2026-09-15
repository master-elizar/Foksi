package com.foksi.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.logic.Agenda
import com.foksi.app.domain.logic.AgendaSection
import com.foksi.app.domain.logic.PlanFilter
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.Category
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItemDetails
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AgendaUiState(
    val sections: List<AgendaSection> = emptyList(),
    val nextUp: PlanItemDetails? = null,
    val all: List<PlanItemDetails> = emptyList(),
    val now: Long = TimeUtils.now(),
    val filter: PlanFilter = PlanFilter.ALL,
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false,
)

/** Shared by the Today, What's next and Tasks screens — they are three views of one list. */
class AgendaViewModel : ViewModel() {

    private val repository = AppGraph.planRepository
    private val interactor = AppGraph.planInteractor

    private val filter = MutableStateFlow(PlanFilter.ALL)
    private val categoryId = MutableStateFlow<Long?>(null)

    private val ticker = flow {
        while (true) {
            emit(TimeUtils.now())
            delay(30_000)
        }
    }

    val state: StateFlow<AgendaUiState> = combine(
        repository.observeAgenda(TimeUtils.startOfDay(TimeUtils.now()) - DAY_BACK),
        repository.observeCategories(),
        AppGraph.settingsRepository.settings,
        combine(filter, categoryId) { f, c -> f to c },
        ticker,
    ) { items, categories, settings, filters, now ->
        val (currentFilter, currentCategory) = filters
        val visible = Agenda.filter(items, currentFilter, now, currentCategory)
        AgendaUiState(
            sections = Agenda.sections(visible, now),
            nextUp = Agenda.nextUp(items, now),
            all = items,
            now = now,
            filter = currentFilter,
            categoryId = currentCategory,
            categories = categories,
            settings = settings,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())

    fun setFilter(value: PlanFilter) {
        filter.value = value
    }

    fun setCategory(value: Long?) {
        categoryId.value = value
    }

    fun toggleDone(id: Long, done: Boolean) {
        viewModelScope.launch { interactor.setCompleted(id, done) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { interactor.delete(id) }
    }

    fun quickSave(type: ItemType, text: String) {
        viewModelScope.launch {
            if (type == ItemType.NOTE) interactor.quickNote(text) else interactor.quickTask(text)
        }
    }

    private companion object {
        /** Keep yesterday visible so overdue items do not vanish at midnight. */
        const val DAY_BACK = 14L * 24 * 60 * 60 * 1000
    }
}
