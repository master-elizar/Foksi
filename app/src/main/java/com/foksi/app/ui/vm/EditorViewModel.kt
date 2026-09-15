package com.foksi.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foksi.app.R
import com.foksi.app.core.TimeUtils
import com.foksi.app.di.AppGraph
import com.foksi.app.domain.model.AdvanceConfig
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.Attachment
import com.foksi.app.domain.model.Category
import com.foksi.app.domain.model.ChecklistItem
import com.foksi.app.domain.model.ItemType
import com.foksi.app.domain.model.PlanItem
import com.foksi.app.domain.model.PlanItemDetails
import com.foksi.app.domain.model.Reminder
import com.foksi.app.domain.logic.Birthdays
import com.foksi.app.domain.model.RepeatMode
import com.foksi.app.domain.model.RepeatRule
import com.foksi.app.domain.model.ReminderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class EditorUiState(
    val details: PlanItemDetails = PlanItemDetails(),
    val settings: AppSettings = AppSettings(),
    val categories: List<Category> = emptyList(),
    val isNew: Boolean = true,
    val hasDate: Boolean = true,
    val errorRes: Int? = null,
    val loaded: Boolean = false,
)

/** Holds the draft while the user edits an event, task or note. */
class EditorViewModel : ViewModel() {

    private val repository = AppGraph.planRepository
    private val interactor = AppGraph.planInteractor

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var initialised = false

    fun init(eventId: Long, type: ItemType, prefillStart: Long?) {
        if (initialised) return
        initialised = true
        viewModelScope.launch {
            val settings = AppGraph.settingsRepository.current()
            val categories = repository.getCategories()
            if (eventId > 0) {
                val loaded = repository.getDetails(eventId)
                if (loaded != null) {
                    _state.value = EditorUiState(
                        details = loaded,
                        settings = settings,
                        categories = categories,
                        isNew = false,
                        hasDate = loaded.item.startAt != null,
                        loaded = true,
                    )
                    return@launch
                }
            }
            val isBirthday = type == ItemType.BIRTHDAY
            val start = prefillStart ?: if (isBirthday) defaultBirthDate() else defaultStart()
            val wantsDate = type == ItemType.EVENT || isBirthday
            _state.value = EditorUiState(
                details = PlanItemDetails(
                    item = PlanItem(
                        type = type,
                        startAt = if (wantsDate) start else null,
                        allDay = isBirthday,
                        durationMinutes = settings.defaultDurationMinutes,
                        repeat = if (isBirthday) {
                            RepeatRule(mode = RepeatMode.YEARLY, interval = 1)
                        } else {
                            RepeatRule()
                        },
                        advance = AdvanceConfig(
                            enabled = settings.advanceDefaultEnabled && type == ItemType.EVENT,
                            perWeek = settings.advanceDefaultPerWeek,
                        ),
                    ),
                    reminders = when {
                        isBirthday -> Birthdays.DEFAULT_REMINDER_OFFSETS.map {
                            Reminder(minutesBefore = it, type = ReminderType.NORMAL)
                        }
                        type == ItemType.EVENT ->
                            listOf(Reminder(minutesBefore = 30, type = ReminderType.NORMAL))
                        else -> emptyList()
                    },
                ),
                settings = settings,
                categories = categories,
                isNew = true,
                hasDate = wantsDate,
                loaded = true,
            )
        }
    }

    /** A birthday picker that opens on "today, thirty years ago" is a friendlier starting point. */
    private fun defaultBirthDate(): Long {
        val today = TimeUtils.toLocalDate(TimeUtils.now()).minusYears(30)
        return TimeUtils.toMillis(today, LocalTime.of(Birthdays.DEFAULT_HOUR, 0))
    }

    private fun defaultStart(): Long {
        val now = TimeUtils.now()
        val rounded = ((now / (30 * 60_000L)) + 2) * (30 * 60_000L)
        return rounded
    }

    private fun updateItem(transform: (PlanItem) -> PlanItem) {
        _state.update { current ->
            current.copy(
                details = current.details.copy(item = transform(current.details.item)),
                errorRes = null,
            )
        }
    }

    fun setTitle(value: String) = updateItem { it.copy(title = value) }
    fun setDescription(value: String) = updateItem { it.copy(description = value) }
    fun setNotes(value: String) = updateItem { it.copy(notes = value) }
    fun setLocation(value: String) = updateItem { it.copy(location = value) }
    fun setUrl(value: String) = updateItem { it.copy(url = value) }
    fun setPriority(value: com.foksi.app.domain.model.Priority) = updateItem { it.copy(priority = value) }
    fun setCategory(value: Long?) = updateItem { it.copy(categoryId = value) }
    fun setColor(value: Int?) = updateItem { it.copy(colorArgb = value) }
    fun setDuration(minutes: Int) = updateItem { it.copy(durationMinutes = minutes.coerceIn(5, 24 * 60)) }
    fun setAllDay(value: Boolean) = updateItem { it.copy(allDay = value) }
    fun setType(value: ItemType) = updateItem { it.copy(type = value) }

    fun setBirthYearKnown(value: Boolean) = updateItem { it.copy(birthYearKnown = value) }

    fun setHasDate(value: Boolean) {
        _state.update { current ->
            val item = current.details.item
            current.copy(
                hasDate = value,
                details = current.details.copy(
                    item = item.copy(startAt = if (value) item.startAt ?: defaultStart() else null)
                )
            )
        }
    }

    fun setDate(date: LocalDate) = updateItem { item ->
        val current = item.startAt ?: defaultStart()
        val time = if (item.type == ItemType.BIRTHDAY) {
            LocalTime.of(Birthdays.DEFAULT_HOUR, 0)
        } else {
            TimeUtils.toLocalDateTime(current).toLocalTime()
        }
        item.copy(startAt = TimeUtils.toMillis(date, time))
    }

    fun setTime(time: LocalTime) = updateItem { item ->
        val current = item.startAt ?: defaultStart()
        item.copy(startAt = TimeUtils.toMillis(TimeUtils.toLocalDate(current), time))
    }

    fun setRepeat(rule: com.foksi.app.domain.model.RepeatRule) = updateItem { it.copy(repeat = rule) }

    fun updateAdvance(transform: (AdvanceConfig) -> AdvanceConfig) =
        updateItem { it.copy(advance = transform(it.advance)) }

    // --- reminders -----------------------------------------------------------------------------

    fun addReminder(minutesBefore: Int, type: ReminderType = ReminderType.NORMAL) {
        _state.update { current ->
            val existing = current.details.reminders
            if (existing.any { it.minutesBefore == minutesBefore && it.type == type }) return@update current
            current.copy(
                details = current.details.copy(
                    reminders = (existing + Reminder(minutesBefore = minutesBefore, type = type))
                        .sortedByDescending { it.minutesBefore }
                )
            )
        }
    }

    fun removeReminder(index: Int) {
        _state.update { current ->
            current.copy(
                details = current.details.copy(
                    reminders = current.details.reminders.filterIndexed { i, _ -> i != index }
                )
            )
        }
    }

    fun updateReminder(index: Int, transform: (Reminder) -> Reminder) {
        _state.update { current ->
            current.copy(
                details = current.details.copy(
                    reminders = current.details.reminders.mapIndexed { i, reminder ->
                        if (i == index) transform(reminder) else reminder
                    }
                )
            )
        }
    }

    // --- checklist -----------------------------------------------------------------------------

    fun addChecklistItem(text: String) {
        if (text.isBlank()) return
        _state.update { current ->
            val items = current.details.checklist
            current.copy(
                details = current.details.copy(
                    checklist = items + ChecklistItem(text = text.trim(), position = items.size)
                )
            )
        }
    }

    fun setChecklistText(index: Int, text: String) = mutateChecklist { items ->
        items.mapIndexed { i, item -> if (i == index) item.copy(text = text) else item }
    }

    fun toggleChecklistItem(index: Int) = mutateChecklist { items ->
        items.mapIndexed { i, item -> if (i == index) item.copy(done = !item.done) else item }
    }

    fun removeChecklistItem(index: Int) = mutateChecklist { items ->
        items.filterIndexed { i, _ -> i != index }
    }

    fun moveChecklistItem(index: Int, delta: Int) = mutateChecklist { items ->
        val target = index + delta
        if (target !in items.indices) items else items.toMutableList().apply {
            val moved = removeAt(index)
            add(target, moved)
        }
    }

    private fun mutateChecklist(transform: (List<ChecklistItem>) -> List<ChecklistItem>) {
        _state.update { current ->
            current.copy(
                details = current.details.copy(
                    checklist = transform(current.details.checklist)
                        .mapIndexed { index, item -> item.copy(position = index) }
                )
            )
        }
    }

    // --- attachments ---------------------------------------------------------------------------

    fun addAttachment(attachment: Attachment) {
        _state.update { current ->
            current.copy(
                details = current.details.copy(attachments = current.details.attachments + attachment)
            )
        }
    }

    fun removeAttachment(index: Int) {
        _state.update { current ->
            current.copy(
                details = current.details.copy(
                    attachments = current.details.attachments.filterIndexed { i, _ -> i != index }
                )
            )
        }
    }

    fun addCategory(name: String, colorArgb: Int) {
        viewModelScope.launch {
            val id = repository.addCategory(name, colorArgb)
            _state.update {
                it.copy(
                    categories = repository.getCategories(),
                    details = it.details.copy(item = it.details.item.copy(categoryId = id)),
                )
            }
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        val current = _state.value
        val item = current.details.item
        if (item.title.isBlank()) {
            _state.update { it.copy(errorRes = R.string.title_required) }
            return
        }
        viewModelScope.launch {
            val normalised = item
                .copy(startAt = if (current.hasDate) item.startAt else null)
                .let { if (it.type == ItemType.BIRTHDAY) Birthdays.asBirthday(it) else it }
            val toSave = current.details.copy(item = normalised)
            val id = interactor.save(toSave)
            onSaved(id)
        }
    }
}
