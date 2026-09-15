package com.foksi.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * One factory for the handful of view models in the app. They all take their dependencies from
 * [com.foksi.app.di.AppGraph], so nothing else needs to be threaded through.
 */
object FoksiViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(AgendaViewModel::class.java) -> AgendaViewModel()
            modelClass.isAssignableFrom(BirthdayViewModel::class.java) -> BirthdayViewModel()
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel()
            modelClass.isAssignableFrom(DetailViewModel::class.java) -> DetailViewModel()
            modelClass.isAssignableFrom(EditorViewModel::class.java) -> EditorViewModel()
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel()
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel()
            else -> throw IllegalArgumentException("Unknown view model: ${modelClass.name}")
        }
        return viewModel as T
    }
}
