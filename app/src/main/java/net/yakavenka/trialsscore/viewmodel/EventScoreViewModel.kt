package net.yakavenka.trialsscore.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.ScoreSummaryRepository
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.IllegalArgumentException

class EventScoreViewModel(
    scoreSummaryRepository: ScoreSummaryRepository
) : ViewModel() {
    val allScores: LiveData<List<RiderScoreSummary>> = scoreSummaryRepository.fetchSummary().asLiveData()

    fun exportReport(uri: Uri, contentResolver: ContentResolver) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use {
                    it.write(
                        ("Overwritten at ${System.currentTimeMillis()}\n")
                            .toByteArray()
                    )
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class Factory(private val riderScoreDao: RiderScoreDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventScoreViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EventScoreViewModel(ScoreSummaryRepository(riderScoreDao)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
