package net.yakavenka.trialsscore.viewmodel

import android.content.ContentResolver
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.*
import net.yakavenka.trialsscore.exchange.CsvExchangeRepository
import java.io.*

private const val TAG = "EventScoreViewModel"

data class RiderStanding(
    val scoreSummary: RiderScoreSummary,
    val standing: Int
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
        return scoreSummary.isFinished()
    }

    fun getProgress(): Int {
        return scoreSummary.getProgress()
    }
}

class EventScoreViewModel(
    scoreSummaryRepository: ScoreSummaryRepository,
    private val sectionScoreRepository: SectionScoreRepository,
    private val importExportService: CsvExchangeRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val allScores: LiveData<List<RiderStanding>> =
        scoreSummaryRepository.fetchSummary().map(this::sortAndEnumerate).asLiveData()

    private fun sortAndEnumerate(summary: List<RiderScoreSummary>): List<RiderStanding> {
        val result = summary.sortedWith(LeaderboardScoreSortOrder(preferencesRepository.fetchPreferences().riderClasses))
        return applyStandings(result)
    }

    // set standing for a sorted list of score summaries
    private fun applyStandings(scores: List<RiderScoreSummary>): List<RiderStanding> {
        val result = mutableListOf<RiderStanding>()
        var prevClass = ""
        var standing = 1
        for (entry: RiderScoreSummary in scores) {
            if (prevClass != entry.riderClass) standing = 1
            result.add(RiderStanding(entry, standing))
            prevClass = entry.riderClass
            standing++
        }
        return result
    }

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

    class Factory(
        private val riderScoreDao: RiderScoreDao,
        private val sharedPreferences: SharedPreferences
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventScoreViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EventScoreViewModel(
                    ScoreSummaryRepository(riderScoreDao),
                    SectionScoreRepository(riderScoreDao),
                    CsvExchangeRepository(),
                    UserPreferencesRepository(sharedPreferences)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
