package net.yakavenka.trialsscore.data

import androidx.room.Entity

@Entity(tableName = "section_score", primaryKeys = ["riderId", "sectionNumber"])
data class SectionScore(
    val riderId: Int,
    val sectionNumber: Int,
    val points: Int
)