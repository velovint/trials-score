package net.yakavenka.trialsscore.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.TrialsScoreApplication
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.data.UserPreferencesRepository

private const val TAG = "ScoreCardViewModel"

class ScoreCardViewModel(
    private val sectionScoreRepository: SectionScoreRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _sectionScores: MutableLiveData<SectionScore.Set> = MutableLiveData()

    val sectionScores: LiveData<SectionScore.Set>
        get() = _sectionScores

    private val _riderInfo: MutableLiveData<RiderScore> = MutableLiveData()

    val riderInfo: LiveData<RiderScore>
        get() = _riderInfo

    val userPreference = userPreferencesRepository.userPreferencesFlow.asLiveData()

    val selectedRiderId = mutableStateOf(-1)

    val selectedLoop: StateFlow<Int> = savedStateHandle.getStateFlow("loop", 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchScores(riderId: Int, loopNumber: Int = 1) {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.flatMapLatest { prefs ->
                sectionScoreRepository.fetchOrInitRiderScore(
                    riderId = riderId,
                    loopNumber = loopNumber,
                    numSections = prefs.numSections,
                    numLoops = prefs.numLoops
                )
            }
                .collect { scores -> _sectionScores.postValue(scores) }
        }
    }

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

    fun loadRiderInfo(riderId: Int) {
        viewModelScope.launch {
            sectionScoreRepository.getRiderInfo(riderId).collect(_riderInfo::postValue)
        }
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val riderScoreDao = (this[APPLICATION_KEY] as TrialsScoreApplication).database.riderScoreDao()
                val sharedPreferences = (this[APPLICATION_KEY] as TrialsScoreApplication).sharedPreferences
                ScoreCardViewModel(SectionScoreRepository(riderScoreDao), UserPreferencesRepository(sharedPreferences), savedStateHandle)
            }
        }
    }

}