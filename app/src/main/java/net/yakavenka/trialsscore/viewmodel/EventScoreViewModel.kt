package net.yakavenka.trialsscore.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
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
    val importContract = ActivityResultContracts.GetContent()
    val exportContract = ActivityResultContracts.CreateDocument("text/csv")

    val allScores: LiveData<Map<String, List<RiderStanding>>> = combine(
            scoreSummaryRepository.fetchSummary(), preferencesRepository.userPreferencesFlow
        ) { summary, prefs ->
            Log.d(TAG, "Fetching allScores")
            return@combine RiderStandingTransformation()
                .invoke(summary, prefs)
                .groupBy { score -> score.riderClass }
        }.asLiveData()

    fun exportReport(uri: Uri, contentResolver: ContentResolver) {
        // more about coroutines https://developer.android.com/kotlin/coroutines
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("EventScoreViewModel", "Exporting results to $uri")
            try {
                val descriptor = contentResolver.openFileDescriptor(uri, "w")
                if (descriptor == null) {
                    Log.e("EventScoreViewModel", "Couldn't open $uri")
                    return@launch
                }
                sectionScoreRepository.fetchFullResults().collect { result ->
                    importExportService.export(result, FileOutputStream(descriptor.fileDescriptor))
                }
                descriptor.close()
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "Failed to open file for export", e)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to open file for export", e)
            }
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
                val preferencesDataStore = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrialsScoreApplication).preferencesDataStore

                EventScoreViewModel(
                    ScoreSummaryRepository(riderScoreDao),
                    SectionScoreRepository(riderScoreDao),
                    CsvExchangeRepository(),
                    UserPreferencesRepository(preferencesDataStore))
            }
        }
    }
}
