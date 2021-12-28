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
            fun createForRider(riderId: Int): List<SectionScore> {
                val sectionScores = mutableListOf<SectionScore>()
                range(1, 11).forEach { sectionNum ->
                    sectionScores.add(SectionScore(riderId, sectionNum, -1))
                }
                return sectionScores
            }
        }

        fun getCleans(): Int {
            return sectionScores.filter { it.points == 0 }.count()
        }

        fun getPoints(): Int {
            return sectionScores.map { it.points }.sum()
        }
    }
}