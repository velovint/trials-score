package net.yakavenka.trialsscore.data

import androidx.room.Entity
import java.util.stream.IntStream.range

@Entity(tableName = "section_score", primaryKeys = ["riderId", "sectionNumber"])
data class SectionScore(
    val riderId: Int,
    val sectionNumber: Int,
    val points: Int
) {
    class SectionScoreList(val sectionScores: List<SectionScore>) {
        
        companion object {
            fun createEmptySet(riderId: Int): List<SectionScore> {
                val sectionScores = mutableListOf<SectionScore>()
                range(1, 11).forEach { sectionNum ->
                    sectionScores.add(SectionScore(riderId, sectionNum, -1))
                }
                return sectionScores
            }
        }
    }
}