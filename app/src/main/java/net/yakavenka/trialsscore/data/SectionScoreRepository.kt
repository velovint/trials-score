package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform


class SectionScoreRepository(
    private val dao: RiderScoreDao
) {
    fun fetchOrInitRiderScore(riderId: Int, numSections: Int): Flow<SectionScore.Set> {
        return dao.sectionScores(riderId)
            .map { SectionScore.Set(it) }
            .transform {
                if (it.sectionScores.isEmpty()) emit(initRiderScoreSet(riderId, numSections))
                else emit(it)
            }
    }

    fun fetchFullResults(): Flow<List<RiderScoreAggregate>> {
        return dao.getAll()
    }

    suspend fun initRiderScoreSet(riderId: Int, numSections: Int): SectionScore.Set {
        val blankScoreSet = SectionScore.Set.createForRider(riderId, numSections)
        dao.insertAll(blankScoreSet.sectionScores)
        return blankScoreSet
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