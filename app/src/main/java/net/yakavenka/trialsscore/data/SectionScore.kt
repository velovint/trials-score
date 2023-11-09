package net.yakavenka.trialsscore.data

import androidx.room.Entity
import java.util.stream.IntStream.range

@Entity(tableName = "section_score", primaryKeys = ["riderId", "loopNumber", "sectionNumber"])
data class SectionScore(
    val riderId: Int,
    val loopNumber: Int,
    val sectionNumber: Int,
    val points: Int
) {
    class Set(val sectionScores: List<SectionScore>) {

        companion object {
            fun createForRider(riderId: Int, numSections: Int, numLoops: Int): Set {
                val sectionScores = mutableListOf<SectionScore>()
                range(1, numSections + 1).forEach { sectionNum ->
                    range(1, numLoops + 1).forEach { loopNum ->
                        sectionScores.add(SectionScore(riderId, loopNum, sectionNum, -1))
                    }
                }
                return Set(sectionScores)
            }
        }

        fun getCleans(): Int {
            return sectionScores.count { it.points == 0 }
        }

        fun getPoints(): Int {
            return sectionScores
                .map { it.points }
                .filter { it >= 0 }
                .sum()
        }

        fun getLoopNumber(): Int {
            return sectionScores.first().loopNumber
        }
    }
}