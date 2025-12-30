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

            fun createForLoop(
                riderId: Int,
                loopNumber: Int,
                numSections: Int,
                existingScores: List<SectionScore>
            ): Set {
                // Map existing scores by section number for O(1) lookup
                val existingBySection = existingScores.associateBy { it.sectionNumber }

                // Create complete set of sections, using existing scores where available
                val completeScores = (1..numSections).map { sectionNum ->
                    existingBySection[sectionNum] ?: SectionScore(
                        riderId = riderId,
                        loopNumber = loopNumber,
                        sectionNumber = sectionNum,
                        points = -1
                    )
                }

                return Set(completeScores)
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