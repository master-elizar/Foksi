package com.foksi.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.logic.Birthdays
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.PlanItemDetails
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BirthdayUiState(
    val nearest: PlanItemDetails? = null,
    val today: List<PlanItemDetails> = emptyList(),
    val thisMonth: List<PlanItemDetails> = emptyList(),
    val later: List<PlanItemDetails> = emptyList(),
    val total: Int = 0,
    val settings: AppSettings = AppSettings(),
    val now: Long = TimeUtils.now(),
    val loaded: Boolean = false,
)

class BirthdayViewModel : ViewModel() {

    private val repository = AppGraph.planRepository
    private val interactor = AppGraph.planInteractor

    private val ticker = flow {
        while (true) {
            emit(TimeUtils.now())
            delay(60_000)
        }
    }

    val state: StateFlow<BirthdayUiState> = combine(
        repository.observeBirthdays(),
        AppGraph.settingsRepository.settings,
        ticker,
    ) { items, settings, now ->
        val sorted = Birthdays.sort(items, now)
        BirthdayUiState(
            nearest = sorted.firstOrNull(),
            today = sorted.filter { Birthdays.daysUntil(it.item, now) == 0L },
            thisMonth = sorted.filter { Birthdays.daysUntil(it.item, now) in 1..31 },
            later = sorted.filter { Birthdays.daysUntil(it.item, now) > 31 },
            total = sorted.size,
            settings = settings,
            now = now,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BirthdayUiState())

    fun delete(id: Long) {
        viewModelScope.launch { interactor.delete(id) }
    }
}
