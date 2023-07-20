package net.yakavenka.trialsscore.viewmodel

import net.yakavenka.trialsscore.data.LeaderboardScoreSortOrder
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.UserPreferences

/**
 * Transform list of scores into a sorted list of rider standings for thes summary screen
 */
class RiderStandingTransformation : (List<RiderScoreSummary>, UserPreferences) -> List<RiderStanding> {

    override fun invoke(
        summary: List<RiderScoreSummary>,
        prefs: UserPreferences
    ): List<RiderStanding> {
        val result =
            summary.sortedWith(LeaderboardScoreSortOrder(prefs.riderClasses, prefs.numSections))
        return applyStandings(result, prefs.numSections)
    }

    private fun applyStandings(
        scores: List<RiderScoreSummary>,
        totalSections: Int
    ): List<RiderStanding> {
        val result = mutableListOf<RiderStanding>()
        var prevClass = ""
        var standing = 1
        for (entry: RiderScoreSummary in scores) {
            if (prevClass != entry.riderClass) standing = 1
            result.add(RiderStanding(entry, standing, totalSections))
            prevClass = entry.riderClass
            standing++
        }
        return result
    }
}