package net.yakavenka.trialsscore.data

import androidx.room.Entity

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
                val sectionScores = (1..numSections).flatMap { sectionNum ->
                    (1..numLoops).map { loopNum ->
                        SectionScore(riderId, loopNum, sectionNum, -1)
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
            require(sectionScores.isNotEmpty()) { "Cannot get loop number from empty score set" }
            return sectionScores.first().loopNumber
        }
    }
}