package net.yakavenka.trialsscore.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import javax.inject.Inject

@HiltViewModel
class EditRiderViewModel @Inject constructor(
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
}

data class RiderDetailsUiState(val entry: RiderScore, val riderClassExpanded: Boolean = false )