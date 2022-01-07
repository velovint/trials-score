package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.*

class ScoreSummaryRepository(private val riderScoreDao: RiderScoreDao) {
    fun fetchSummary(): Flow<List<RiderScoreSummary>> {
        return riderScoreDao.fetchSummary().map { it.sortedWith(LeaderboardScoreSortOrder()) }
    }

    fun fetchFullResults(): Flow<List<Any>> = flow {
        emit(Collections.emptyList())
    }

}