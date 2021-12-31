package net.yakavenka.trialsscore.data

import androidx.room.Entity
import java.util.stream.IntStream.range

@Entity(tableName = "section_score", primaryKeys = ["riderId", "sectionNumber"])
data class SectionScore(
    val riderId: Int,
    val sectionNumber: Int,
    val points: Int
) {
    class Set(val sectionScores: List<SectionScore>) {

        companion object {
            const val TOTAL_SECTIONS = 10
            fun createForRider(riderId: Int): Set {
                val sectionScores = mutableListOf<SectionScore>()
                range(1, TOTAL_SECTIONS + 1).forEach { sectionNum ->
                    sectionScores.add(SectionScore(riderId, sectionNum, -1))
                }
                return Set(sectionScores)
            }
        }

        fun getCleans(): Int {
            return sectionScores.filter { it.points == 0 }.count()
        }

        fun getPoints(): Int {
            return sectionScores
                .map { it.points }
                .filter { it >= 0 }
                .sum()
        }
    }
}