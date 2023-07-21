package net.yakavenka.trialsscore.data

class LeaderboardScoreSortOrder(private val riderClasses: Set<String>, private val totalSections: Int) : Comparator<RiderScoreSummary> {
    override fun compare(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        val classComparison = compareClass(left, right)
        if (classComparison != 0) return classComparison

        // finished always before not finished
        val finishedComparison = compareFinished(left, right)
        if (finishedComparison != 0) return finishedComparison

        // not finished compared by name
        if (left.sectionsRidden < totalSections) return compareNames(left, right)

        // finished compared by points
        val pointsComparison = comparePoints(left, right)
        if (pointsComparison != 0) return pointsComparison

        // same points compared by cleans
        return compareCleans(left, right)
    }

    private fun compareClass(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        if (left.riderClass == right.riderClass) return 0
        return riderClasses.indexOf(left.riderClass)
            .compareTo(riderClasses.indexOf(right.riderClass))
    }

    private fun compareCleans(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        return right.numCleans.compareTo(left.numCleans) // note reversal
    }

    private fun compareFinished(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        val rightFinished = right.sectionsRidden == totalSections
        val leftFinished = left.sectionsRidden == totalSections
        return rightFinished.compareTo(leftFinished) // note reversal
    }

    private fun compareNames(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        return left.riderName.compareTo(right.riderName)
    }

    private fun comparePoints(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        return left.points.compareTo(right.points)
    }

}