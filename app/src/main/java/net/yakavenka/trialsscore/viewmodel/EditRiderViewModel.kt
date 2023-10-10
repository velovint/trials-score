package net.yakavenka.trialsscore.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.TrialsScoreApplication
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.UserPreferencesRepository

class EditRiderViewModel(
    private val riderScoreDao: RiderScoreDao,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val userPreference = userPreferencesRepository.userPreferencesFlow.asLiveData()
    var riderClass by mutableStateOf("")
        private set
    var riderClassExpanded by mutableStateOf(false)
        private set
    var riderName by mutableStateOf("")
        private set
    private var riderId: Int = 0

    fun addRider(name: String, riderClass: String) {
        viewModelScope.launch {
            riderScoreDao.addRider(RiderScore(name = name, riderClass = riderClass))
        }
    }

    fun loadRider(id: Int) {
        viewModelScope.launch {
            riderScoreDao.getRider(id)
                .collect { riderScore ->
                    riderId = id
                    riderName = riderScore.name
                    riderClass = riderScore.riderClass
            }
        }
    }

    fun updateRider(id: Int, name: String, riderClass: String) {
        viewModelScope.launch {
            riderScoreDao.updateRider(RiderScore(id = id, name = name, riderClass = riderClass))
        }
    }

    fun updateRiderName(name: String) {
        riderName = name
    }

    fun toggleRiderClassExpanded(expanded: Boolean) {
        riderClassExpanded = expanded
    }

    fun updateRiderClass(riderClass: String) {
        this.riderClass = riderClass
        toggleRiderClassExpanded(false)
    }

    fun saveRider() {
        if (riderId > 0) {
            updateRider(riderId, riderName, riderClass)
            return
        } else {
            addRider(riderName, riderClass)
        }
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val riderScoreDao = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrialsScoreApplication).database.riderScoreDao()
                val sharedPreferences = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrialsScoreApplication).sharedPreferences
                EditRiderViewModel(riderScoreDao, UserPreferencesRepository(sharedPreferences))
            }
        }
    }
}