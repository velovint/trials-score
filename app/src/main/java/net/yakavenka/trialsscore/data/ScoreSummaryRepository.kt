package net.yakavenka.trialsscore.data

import android.util.Log
import kotlinx.coroutines.flow.Flow

class ScoreSummaryRepository(
    private val riderScoreDao: RiderScoreDao
) {
    fun fetchSummary(): Flow<List<RiderScoreSummary>> {
        Log.d("ScoreSummaryRepository", "fetchSummary()")
        return riderScoreDao.fetchSummary()
    }


}