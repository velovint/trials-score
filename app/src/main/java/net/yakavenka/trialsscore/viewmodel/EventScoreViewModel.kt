package net.yakavenka.trialsscore.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.TrialsScoreApplication
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.ScoreSummaryRepository
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import net.yakavenka.trialsscore.exchange.CsvExchangeRepository
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "EventScoreViewModel"

data class RiderStanding(
    val scoreSummary: RiderScoreSummary,
    val standing: Int,
    val totalSections: Int
) {
    val riderClass: String
        get() = scoreSummary.riderClass

    val riderId: Int
        get() = scoreSummary.riderId

    val riderName: String
        get() = scoreSummary.riderName

    val points: Int
        get() = scoreSummary.points

    val numCleans: Int
        get() = scoreSummary.numCleans

    fun isFinished(): Boolean {
        return scoreSummary.sectionsRidden == totalSections
    }

    fun getProgress(): Int {
        return scoreSummary.sectionsRidden * 100 / totalSections
    }
}

class EventScoreViewModel(
    scoreSummaryRepository: ScoreSummaryRepository,
    private val sectionScoreRepository: SectionScoreRepository,
    private val importExportService: CsvExchangeRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val allScores: LiveData<List<RiderStanding>> =
        scoreSummaryRepository.fetchSummary()
            .map { summary -> RiderStandingTransformation().invoke(summary, preferencesRepository.fetchPreferences()) }
            .asLiveData()

    fun exportReport(uri: Uri, contentResolver: ContentResolver) {
        try {
            val descriptor = contentResolver.openFileDescriptor(uri, "w")
            if (descriptor == null) {
                Log.e("EventScoreViewModel", "Couldn't open $uri")
                return
            }

            viewModelScope.launch(Dispatchers.IO) {
                sectionScoreRepository.fetchFullResults().collect { result ->
                    importExportService.export(result, FileOutputStream(descriptor.fileDescriptor))
                }
                descriptor.close()
            }
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Failed to open file for export", e)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open file for export", e)
        }
    }

    fun importRiders(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Can't open file $uri")
            }
            importExportService.importRiders(inputStream!!).collect { rider ->
                sectionScoreRepository.addRider(rider)
            }
            inputStream.close()
        }

    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            sectionScoreRepository.purge()
        }
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val riderScoreDao = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrialsScoreApplication).database.riderScoreDao()
                val sharedPreferences = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrialsScoreApplication).sharedPreferences
                EventScoreViewModel(
                    ScoreSummaryRepository(riderScoreDao),
                    SectionScoreRepository(riderScoreDao),
                    CsvExchangeRepository(),
                    UserPreferencesRepository(sharedPreferences))
            }
        }
    }
}
