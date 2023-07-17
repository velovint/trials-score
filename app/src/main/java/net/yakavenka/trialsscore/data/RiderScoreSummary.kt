package net.yakavenka.trialsscore.data

import androidx.room.ColumnInfo

data class RiderScoreSummary(
    @ColumnInfo(name = "id") val riderId: Int,
    @ColumnInfo(name = "name") val riderName: String,
    @ColumnInfo(name = "class") val riderClass: String,
    @ColumnInfo(name = "sections_ridden") val sectionsRidden: Int,
    @ColumnInfo(name = "points") val points: Int,
    @ColumnInfo(name = "cleans") val numCleans: Int,
    @ColumnInfo(name = "total_sections") val totalSections: Int
)