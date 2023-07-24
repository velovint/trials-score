package net.yakavenka.trialsscore.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreDao

class EditRiderViewModel(
    val riderScoreDao: RiderScoreDao
) : ViewModel() {

    fun addRider(name: String, riderClass: String) {
        viewModelScope.launch {
            riderScoreDao.addRider(RiderScore(name = name, riderClass = riderClass))
        }
    }

    fun loadRider(id: Int): LiveData<RiderScore> {
        return riderScoreDao.getRider(id).asLiveData()
    }

    fun updateRider(id: Int, name: String, riderClass: String) {
        viewModelScope.launch {
            riderScoreDao.updateRider(RiderScore(id = id, name = name, riderClass = riderClass))
        }
    }

    class Factory(private val riderScoreDao: RiderScoreDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditRiderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EditRiderViewModel(riderScoreDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}