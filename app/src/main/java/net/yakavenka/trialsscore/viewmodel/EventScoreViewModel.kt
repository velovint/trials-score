package net.yakavenka.trialsscore.viewmodel

import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.ScoreSummaryRepository
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import net.yakavenka.trialsscore.exchange.CsvExchangeRepository
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "EventScoreViewModel"

data class RiderStanding(
    val scoreSummary: RiderScoreSummary,
    private val _standing: Int,
    val numSections: Int,
    val numLoops: Int
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

    val standing: String
        get() {
            val symbolMap = mapOf(
                1 to "⠂",
                2 to "⠅",
                3 to "⠇",
                4 to "⠭"
            )
            if (loopsComplete == numLoops) return _standing.toString()
            if (symbolMap.containsKey(loopsComplete)) return symbolMap[loopsComplete]!!
            return "*"
        }

    val loopsComplete: Int
        get() = scoreSummary.sectionsRidden.floorDiv(numSections)
}

@HiltViewModel
class EventScoreViewModel @Inject constructor(
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

    fun exportReport(uri: Uri) {
        // more about coroutines https://developer.android.com/kotlin/coroutines
        viewModelScope.launch {
            Log.d(TAG, "Exporting results to $uri")
            try {
                val result = sectionScoreRepository.fetchFullResults().first()
                importExportService.exportToUri(result, uri)
                Log.d(TAG, "CSV Export complete")
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "Failed to open file for export", e)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to open file for export", e)
            }
        }
    }

    fun importRiders(uri: Uri) {
        viewModelScope.launch {
            try {
                val riders = importExportService.importRidersFromUri(uri)
                riders.forEach { rider ->
                    sectionScoreRepository.addRider(rider)
                }
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "Can't open file $uri", e)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to read file for import", e)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            sectionScoreRepository.purge()
        }
    }
}
