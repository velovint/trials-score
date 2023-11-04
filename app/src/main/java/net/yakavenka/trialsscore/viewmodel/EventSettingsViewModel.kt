package net.yakavenka.trialsscore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import javax.inject.Inject

@HiltViewModel
class EventSettingsViewModel @Inject constructor(
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
}

