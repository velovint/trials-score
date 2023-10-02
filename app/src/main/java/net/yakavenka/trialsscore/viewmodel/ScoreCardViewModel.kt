package net.yakavenka.trialsscore.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.data.UserPreferencesRepository

private const val TAG = "ScoreCardViewModel"

class ScoreCardViewModel(
    private val sectionScoreRepository: SectionScoreRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _sectionScores: MutableLiveData<SectionScore.Set> = MutableLiveData()

    val sectionScores: LiveData<SectionScore.Set>
        get() = _sectionScores

    private val _riderInfo: MutableLiveData<RiderScore> = MutableLiveData()

    val riderInfo: LiveData<RiderScore>
        get() = _riderInfo

    val userPreference = userPreferencesRepository.userPreferencesFlow.asLiveData()

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

    class Factory(
        private val riderScoreDao: RiderScoreDao,
        private val sharedPreferences: SharedPreferences
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScoreCardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ScoreCardViewModel(SectionScoreRepository(riderScoreDao), UserPreferencesRepository(sharedPreferences)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}