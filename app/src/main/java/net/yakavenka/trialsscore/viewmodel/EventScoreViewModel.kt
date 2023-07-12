package net.yakavenka.trialsscore.viewmodel

import android.content.ContentResolver
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.*
import net.yakavenka.trialsscore.exchange.CsvExchangeRepository
import java.io.*

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
            // TODO inputStream.close()
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
                    ScoreSummaryRepository(riderScoreDao, sharedPreferences),
                    SectionScoreRepository(riderScoreDao),
                    CsvExchangeRepository()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
