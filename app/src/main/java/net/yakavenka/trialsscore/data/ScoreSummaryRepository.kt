package net.yakavenka.trialsscore.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreSummaryRepository @Inject constructor(
    private val riderScoreDao: RiderScoreDao
) {
    fun fetchSummary(): Flow<List<RiderScoreSummary>> {
        Log.d("ScoreSummaryRepository", "fetchSummary()")
        return riderScoreDao.fetchSummary()
    }


}