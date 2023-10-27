package net.yakavenka.trialsscore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.TrialsScoreApplication
import net.yakavenka.trialsscore.data.UserPreferencesRepository

class EventSettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel(
) {
    val userPreferences = userPreferencesRepository.userPreferencesFlow.asLiveData()

    fun updateNumSections(numSections: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateNumSections(numSections)
        }
    }

    fun updateNumLoops(numLoops: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateNumLoops(numLoops)
        }
    }

    fun updateRiderClasses(riderClasses: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateClasses(
                riderClasses.split(",").map { it.trim() }.toSet())
        }
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val preferencesDataStore =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrialsScoreApplication).preferencesDataStore

                EventSettingsViewModel(
                    UserPreferencesRepository(preferencesDataStore)
                )
            }
        }
    }
}

