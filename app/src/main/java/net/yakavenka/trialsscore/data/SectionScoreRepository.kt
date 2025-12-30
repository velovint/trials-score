package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SectionScoreRepository @Inject constructor(
    private val dao: RiderScoreDao
) {
    fun fetchOrInitRiderScore(riderId: Int, loopNumber: Int = 1, numSections: Int, numLoops: Int): Flow<SectionScore.Set> {
        return dao.sectionScores(riderId, loopNumber)
            .map { existingScores ->
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

                SectionScore.Set(completeScores)
            }
    }

    fun fetchFullResults(): Flow<List<RiderScoreAggregate>> {
        return dao.getAll()
    }

    suspend fun updateSectionScore(updatedRecord: SectionScore) {
        dao.updateSectionScore(updatedRecord)
    }

    suspend fun deleteRiderScores(riderId: Int) {
        dao.deleteRiderScores(riderId)
    }

    suspend fun addRider(rider: RiderScore) {
        dao.addRider(rider)
    }

    suspend fun purge() {
        dao.deleteAllScores()
        dao.deleteAllRiders()
    }

    fun getRiderInfo(riderId: Int): Flow<RiderScore> {
        return dao.getRider(riderId)
    }
}