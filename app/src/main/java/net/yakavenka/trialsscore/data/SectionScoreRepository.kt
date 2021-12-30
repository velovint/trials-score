package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.*


class SectionScoreRepository(
    private val dao: RiderScoreDao
) {
    fun fetchOrInitRiderScore(riderId: Int): Flow<SectionScore.Set> {
        return dao.sectionScores(riderId)
            .map { SectionScore.Set(it) }
            .transform {
                if (it.sectionScores.isEmpty()) emit(initRiderScoreSet(riderId))
                else emit(it)
            }
    }

    suspend fun initRiderScoreSet(riderId: Int): SectionScore.Set {
        val blankScoreSet = SectionScore.Set.createForRider(riderId)
        dao.insertAll(blankScoreSet.sectionScores)
        return blankScoreSet
    }
}