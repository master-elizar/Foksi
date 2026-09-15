package com.foksi.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.ScheduledNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val details: PlanItemDetails? = null,
    val settings: AppSettings = AppSettings(),
    val upcomingReminders: List<ScheduledNotification> = emptyList(),
    val missing: Boolean = false,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailViewModel : ViewModel() {

    private val repository = AppGraph.planRepository
    private val interactor = AppGraph.planInteractor

    private val eventId = MutableStateFlow(0L)
    private val pending = MutableStateFlow<List<ScheduledNotification>>(emptyList())

    val state: StateFlow<DetailUiState> = combine(
        eventId.flatMapLatest { id -> repository.observeDetails(id) },
        AppGraph.settingsRepository.settings,
        pending.asStateFlow(),
    ) { details, settings, reminders ->
        DetailUiState(
            details = details,
            settings = settings,
            upcomingReminders = reminders,
            missing = details == null && eventId.value != 0L,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun load(id: Long) {
        if (eventId.value == id) return
        eventId.value = id
        refreshPending()
    }

    private fun refreshPending() {
        viewModelScope.launch {
            pending.value = repository.getPendingForEvent(eventId.value).take(6)
        }
    }

    fun toggleChecklistItem(itemId: Long, done: Boolean) {
        viewModelScope.launch { interactor.setChecklistDone(itemId, done) }
    }

    fun setCompleted(done: Boolean) {
        viewModelScope.launch {
            interactor.setCompleted(eventId.value, done)
            refreshPending()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            interactor.delete(eventId.value)
            onDone()
        }
    }

    fun duplicate(onDone: (Long) -> Unit) {
        viewModelScope.launch {
            interactor.duplicate(eventId.value)?.let(onDone)
        }
    }
}
