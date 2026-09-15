package com.foksi.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.logic.RecurrenceEngine
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.PlanItemDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarViewMode { MONTH, WEEK, DAY, AGENDA }

data class CalendarEntry(val details: PlanItemDetails, val start: Long)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val entriesByDate: Map<LocalDate, List<CalendarEntry>> = emptyMap(),
    val settings: AppSettings = AppSettings(),
)

class CalendarViewModel : ViewModel() {

    private val repository = AppGraph.planRepository
    private val interactor = AppGraph.planInteractor

    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())
    private val mode = MutableStateFlow(CalendarViewMode.MONTH)

    val state: StateFlow<CalendarUiState> = combine(
        repository.observeAgenda(from = 0L, limit = 3000),
        AppGraph.settingsRepository.settings,
        month,
        selected,
        mode,
    ) { items, settings, currentMonth, selectedDate, viewMode ->
        CalendarUiState(
            month = currentMonth,
            selectedDate = selectedDate,
            viewMode = viewMode,
            entriesByDate = expand(items, currentMonth, viewMode, selectedDate),
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    /** Expands repeat rules into the visible window so recurring events show on every day. */
    private fun expand(
        items: List<PlanItemDetails>,
        month: YearMonth,
        viewMode: CalendarViewMode,
        selectedDate: LocalDate,
    ): Map<LocalDate, List<CalendarEntry>> {
        val windowStart = when (viewMode) {
            CalendarViewMode.DAY -> selectedDate.minusDays(1)
            CalendarViewMode.WEEK -> selectedDate.minusDays(10)
            else -> month.atDay(1).minusDays(7)
        }
        val windowEnd = when (viewMode) {
            CalendarViewMode.DAY -> selectedDate.plusDays(1)
            CalendarViewMode.WEEK -> selectedDate.plusDays(10)
            CalendarViewMode.AGENDA -> month.atEndOfMonth().plusMonths(3)
            else -> month.atEndOfMonth().plusDays(7)
        }
        val from = TimeUtils.startOfDay(windowStart)
        val to = TimeUtils.endOfDay(windowEnd)

        val result = sortedMapOf<LocalDate, MutableList<CalendarEntry>>()
        items.forEach { details ->
            if (details.item.startAt == null) return@forEach
            RecurrenceEngine.occurrences(details.item, from, to, max = 400).forEach { occurrence ->
                val date = TimeUtils.toLocalDate(occurrence)
                result.getOrPut(date) { mutableListOf() }.add(CalendarEntry(details, occurrence))
            }
        }
        return result.mapValues { (_, value) -> value.sortedBy { it.start } }
    }

    fun selectDate(date: LocalDate) {
        selected.value = date
        if (YearMonth.from(date) != month.value) month.value = YearMonth.from(date)
    }

    fun showMonth(value: YearMonth) {
        month.value = value
    }

    fun setViewMode(value: CalendarViewMode) {
        mode.value = value
    }

    fun today() {
        val now = LocalDate.now()
        selected.value = now
        month.value = YearMonth.from(now)
    }

    fun toggleDone(id: Long, done: Boolean) {
        viewModelScope.launch { interactor.setCompleted(id, done) }
    }
}
