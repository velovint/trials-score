package net.yakavenka.trialsscore.viewmodel

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.SectionScore
import java.lang.IllegalArgumentException

class ScoreCardViewModel(
    private val riderScoreDao: RiderScoreDao
) : ViewModel() {
    private val TAG = "EventScore"

    private val _scoreCard: MutableLiveData<RiderScoreAggregate> = MutableLiveData()

    val scoreCard: LiveData<RiderScoreAggregate>
        get() = _scoreCard

    private val _sectionScores: MutableLiveData<SectionScore.Set> = MutableLiveData()

    val sectionScores: LiveData<SectionScore.Set>
        get() = _sectionScores

    fun fetchScores(riderId: Int) {
        viewModelScope.launch {
            riderScoreDao.sectionScores(riderId).collect { sectionScores ->
                val riderScore :RiderScoreAggregate
                if (sectionScores.isEmpty()) {
                    val emptyScoreCollection: List<SectionScore> = SectionScore.Set.createForRider(riderId)
                    riderScoreDao.insertAll(emptyScoreCollection)
                    riderScore = RiderScoreAggregate(RiderScore(0, "NA"), emptyScoreCollection)
                } else {
                    riderScore = RiderScoreAggregate(RiderScore(0, "NA"), sectionScores)
                }
                _scoreCard.postValue(riderScore)
                _sectionScores.postValue(SectionScore.Set(riderScore.sections))
            }
        }
    }

    fun updateSectionScore(updatedRecord: SectionScore) {
        Log.d(TAG, "Updating section score $updatedRecord")
        viewModelScope.launch {
            riderScoreDao.updateSectionScore(updatedRecord)
        }
        val newScores =
            _scoreCard.value?.sections?.map { original ->
                if (original.sectionNumber == updatedRecord.sectionNumber)
                    updatedRecord
                else original
            }
        newScores?.let {
            _scoreCard.postValue(_scoreCard.value?.copy(sections = it))
        }
    }

    fun clearScores(riderId: Int) {
        viewModelScope.launch {
            riderScoreDao.deleteRiderScores(riderId)
        }
    }

    class Factory(private val riderScoreDao: RiderScoreDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScoreCardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ScoreCardViewModel(riderScoreDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}