package net.yakavenka.trialsscore.viewmodel

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import javax.inject.Inject

private const val TAG = "ScoreCardViewModel"

@HiltViewModel
class ScoreCardViewModel @Inject constructor(
    private val sectionScoreRepository: SectionScoreRepository,
    userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val selectedRiderId: Int = checkNotNull(savedStateHandle["riderId"]) as Int

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _sectionScores =
        userPreferencesRepository.userPreferencesFlow.flatMapLatest { prefs ->
            sectionScoreRepository.fetchOrInitRiderScore(
                riderId = selectedRiderId,
                loopNumber = selectedLoop,
                numSections = prefs.numSections,
                numLoops = prefs.numLoops
            )
        }

    val sectionScores = _sectionScores.asLiveData()

    val userPreference = userPreferencesRepository.userPreferencesFlow.asLiveData()

    val riderInfo = sectionScoreRepository.getRiderInfo(selectedRiderId).asLiveData()

    val selectedLoop: Int = checkNotNull(savedStateHandle["loop"]) as Int

    fun updateSectionScore(updatedRecord: SectionScore) {
        Log.d(TAG, "Updating section score $updatedRecord")
        viewModelScope.launch {
            sectionScoreRepository.updateSectionScore(updatedRecord)
        }
    }

    fun clearScores(riderId: Int) {
        viewModelScope.launch {
            sectionScoreRepository.deleteRiderScores(riderId)
        }
    }
}