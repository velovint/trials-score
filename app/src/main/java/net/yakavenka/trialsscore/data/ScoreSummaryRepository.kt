package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScoreSummaryRepository(
    private val riderScoreDao: RiderScoreDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    fun fetchSummary(): Flow<List<RiderScoreSummary>> {
        return riderScoreDao.fetchSummary().map {
            it.forEach { summary -> summary.totalSections = userPreferencesRepository.fetchPreferences().numSections }
            // consider alternative approach to simplify whole grouping/sorting/enumerating logic
            // group by class, sort all classes, set standing by index in the list
            // optionally flatten back
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
            if (prevClass != entry.riderClass) standing = 1
            entry.standing = standing
            prevClass = entry.riderClass
            standing++
        }
    }

}