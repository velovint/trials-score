package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScoreSummaryRepository(private val riderScoreDao: RiderScoreDao) {
    fun fetchSummary(): Flow<List<RiderScoreSummary>> {
        return riderScoreDao.fetchSummary().map {
            // group by class
            // sort all classes
            // set standing
            // flatten
            val result = it.sortedWith(LeaderboardScoreSortOrder())
            enumerate(result)
            result
        }
    }

    // set standing for a sorted list of score summaries
    private fun enumerate(result: List<RiderScoreSummary>) {
        var prevClass = ""
        var standing = 1
        for (entry: RiderScoreSummary in result) {
            if (!prevClass.equals(entry.riderClass)) standing = 1
            entry.standing = standing
            prevClass = entry.riderClass
            standing++
        }
    }

}