package com.minim.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minim.launcher.MinimApplication

class ViewModelFactory(private val app: MinimApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                appRepository = app.appRepository,
                settingsRepository = app.settingsRepository,
                suggestionRepository = app.suggestionRepository,
                spacesRepository = app.spacesRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
