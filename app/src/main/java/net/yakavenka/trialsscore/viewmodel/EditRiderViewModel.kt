package net.yakavenka.trialsscore.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.TrialsScoreApplication
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.UserPreferencesRepository

class EditRiderViewModel(
    private val riderScoreDao: RiderScoreDao,
    userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val userPreference = userPreferencesRepository.userPreferencesFlow.asLiveData()

    private val riderId: Int = savedStateHandle["riderId"] ?: 0
    var riderInfoState by mutableStateOf(RiderDetailsUiState(RiderScore(0, "", ""), false))
        private set

    init {
        if (riderId != 0) {
            viewModelScope.launch {
                riderInfoState = riderScoreDao.getRider(riderId)
                    .map { RiderDetailsUiState(it, false) }
                    .first()
            }
        }
    }

    fun toggleRiderClassExpanded(expanded: Boolean) {
        riderInfoState = riderInfoState.copy(riderClassExpanded = expanded)
    }

    suspend fun saveRider() {
        if (riderId > 0) {
            riderScoreDao.updateRider(
                RiderScore(
                    id = riderId,
                    name = riderInfoState.entry.name,
                    riderClass = riderInfoState.entry.riderClass
                )
            )
        } else {
            riderScoreDao.addRider(
                RiderScore(
                    name = riderInfoState.entry.name,
                    riderClass = riderInfoState.entry.riderClass
                )
            )
        }
    }

    fun updateUiState(riderScore: RiderScore) {
        riderInfoState = riderInfoState.copy(entry = riderScore)
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val riderScoreDao = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrialsScoreApplication).database.riderScoreDao()
                val prefsDatastore = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrialsScoreApplication).preferencesDataStore
                val savedStateHandle = createSavedStateHandle()
                EditRiderViewModel(riderScoreDao, UserPreferencesRepository(prefsDatastore), savedStateHandle)
            }
        }
    }
}

data class RiderDetailsUiState(val entry: RiderScore, val riderClassExpanded: Boolean = false )