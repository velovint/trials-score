package net.yakavenka.trialsscore.data

import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel

class LeaderboardScoreSortOrder : Comparator<RiderScoreSummary> {
    override fun compare(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        val classComparison = compareClass(left, right)
        if (classComparison != 0) return classComparison

        // finished always before not finished
        val finishedComparison = compareFinished(left, right)
        if (finishedComparison != 0) return finishedComparison

        // not finished compared by name
        if (!left.isFinished()) return compareNames(left, right)

        // finished compared by points
        val pointsComparison = comparePoints(left, right)
        if (pointsComparison != 0) return pointsComparison

        // same points compared by cleans
        return compareCleans(left, right)
    }

    private fun compareClass(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        if (left.riderClass.equals(right.riderClass)) return 0
        return EditRiderViewModel.RIDER_CLASS_OPTIONS.indexOf(left.riderClass)
            .compareTo(EditRiderViewModel.RIDER_CLASS_OPTIONS.indexOf(right.riderClass))
    }

    private fun compareCleans(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        return left.numCleans.compareTo(right.numCleans)
    }

    private fun compareFinished(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        return right.isFinished().compareTo(left.isFinished()) // note reversal
    }

    private fun compareNames(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        return left.riderName.compareTo(right.riderName)
    }

    private fun comparePoints(left: RiderScoreSummary, right: RiderScoreSummary): Int {
        return left.points.compareTo(right.points)
    }

}