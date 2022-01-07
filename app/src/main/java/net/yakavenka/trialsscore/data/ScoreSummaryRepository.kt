package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScoreSummaryRepository(private val riderScoreDao: RiderScoreDao) {
    fun fetchSummary(): Flow<List<RiderScoreSummary>> {
        return riderScoreDao.fetchSummary().map { it.sortedWith(LeaderboardScoreSortOrder()) }
    }

}