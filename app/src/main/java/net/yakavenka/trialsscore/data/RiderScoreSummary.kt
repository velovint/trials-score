package net.yakavenka.trialsscore.data

import androidx.room.ColumnInfo
import androidx.room.Ignore

data class RiderScoreSummary(
    @ColumnInfo(name = "id") val riderId: Int,
    @ColumnInfo(name = "name") val riderName: String,
    @ColumnInfo(name = "class") val riderClass: String,
    @ColumnInfo(name = "sections_ridden") val sectionsRidden: Int,
    @ColumnInfo(name = "points") val points: Int,
    @ColumnInfo(name = "cleans") val numCleans: Int
) {
    /**
     * Holder for rider standing
     *
     * Make sure to check isFinished() before using it
     */
    @Ignore
    var standing: Int = 0

    fun isFinished(): Boolean {
        return sectionsRidden == SectionScore.Set.TOTAL_SECTIONS
    }

    fun getProgress(): Int {
        return sectionsRidden * 100 / SectionScore.Set.TOTAL_SECTIONS
    }

}