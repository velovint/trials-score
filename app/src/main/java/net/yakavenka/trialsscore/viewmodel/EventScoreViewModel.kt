package net.yakavenka.trialsscore.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.ScoreSummaryRepository
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.exchange.CsvExchangeRepository
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "EventScoreViewModel"

class EventScoreViewModel(
    scoreSummaryRepository: ScoreSummaryRepository,
    private val sectionScoreRepository: SectionScoreRepository,
    private val importExportService: CsvExchangeRepository
) : ViewModel() {
    val allScores: LiveData<List<RiderScoreSummary>> = scoreSummaryRepository.fetchSummary().asLiveData()

    fun exportReport(uri: Uri, contentResolver: ContentResolver) {
        try {
            val descriptor = contentResolver.openFileDescriptor(uri, "w")
            if (descriptor == null) {
                Log.e("EventScoreViewModel", "Couldn't open $uri")
                return
            }

            viewModelScope.launch {
                sectionScoreRepository.fetchFullResults().collect { result ->
                    importExportService.export(result, FileOutputStream(descriptor.fileDescriptor))
                }
            }
            descriptor.close()
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Failed to open file for export", e)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open file for export", e)
        }
    }

    class Factory(private val riderScoreDao: RiderScoreDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventScoreViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EventScoreViewModel(
                    ScoreSummaryRepository(riderScoreDao),
                    SectionScoreRepository(riderScoreDao),
                    CsvExchangeRepository()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
