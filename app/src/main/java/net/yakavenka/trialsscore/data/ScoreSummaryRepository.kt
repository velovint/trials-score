package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow

class ScoreSummaryRepository(private val riderScoreDao: RiderScoreDao) {
    fun fetchSummary(): Flow<List<RiderScoreSummary>> {
        return riderScoreDao.fetchSummary()
    }
}