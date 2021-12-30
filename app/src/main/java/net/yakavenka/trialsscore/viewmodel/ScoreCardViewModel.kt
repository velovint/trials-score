package net.yakavenka.trialsscore.viewmodel

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.SectionScoreRepository

private const val TAG = "ScoreCardViewModel"

class ScoreCardViewModel(
    private val sectionScoreRepository: SectionScoreRepository
) : ViewModel() {

    private val _sectionScores: MutableLiveData<SectionScore.Set> = MutableLiveData()

    val sectionScores: LiveData<SectionScore.Set>
        get() = _sectionScores

    fun fetchScores(riderId: Int) {
        viewModelScope.launch {
            sectionScoreRepository
                .fetchOrInitRiderScore(riderId)
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

    class Factory(private val riderScoreDao: RiderScoreDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScoreCardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ScoreCardViewModel(SectionScoreRepository(riderScoreDao)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}