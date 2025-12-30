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
                SectionScore.Set.createForLoop(
                    riderId = riderId,
                    loopNumber = loopNumber,
                    numSections = numSections,
                    existingScores = existingScores
                )
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