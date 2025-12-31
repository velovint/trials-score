package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Test

class SectionScoreRepositoryTest {

    @Test
    fun fetchOrInitRiderScore_returnsOnlyRequestedLoop_whenScoresExist() = runBlocking {
        // Given: Rider with scores for multiple loops already in database
        val riderId = 1
        val numSections = 10
        val numLoops = 3
        val requestedLoop = 2

        val dao = RiderScoreDaoFake()
        val allScores = SectionScore.Set.createForRider(riderId, numSections, numLoops)
        dao.existingScores.addAll(allScores.sectionScores)

        val sut = SectionScoreRepository(dao)

        // When: Fetching scores for loop 2
        val result = sut.fetchOrInitRiderScore(riderId, requestedLoop, numSections).first()

        // Then: Should return only loop 2 scores (10 sections)
        assertThat(result.sectionScores, hasSize(numSections))
        assertThat(result.getLoopNumber(), equalTo(requestedLoop))
    }

    @Test
    fun fetchOrInitRiderScore_returnsCompleteSet_whenPartiallyScored() = runBlocking {
        val riderId = 1
        val numSections = 3
        val requestedLoop = 1

        val dao = RiderScoreDaoFake()
        dao.existingScores.add(SectionScore(riderId, requestedLoop, sectionNumber = 2, points = 5))

        val sut = SectionScoreRepository(dao)

        val result = sut.fetchOrInitRiderScore(riderId, requestedLoop, numSections).first()

        assertThat(result.sectionScores, hasSize(3))
        assertThat(result.sectionScores[0].points, equalTo(-1))
        assertThat(result.sectionScores[1].points, equalTo(5))
        assertThat(result.sectionScores[2].points, equalTo(-1))
    }

    @Test
    fun fetchOrInitRiderScore_returnsOnlyRequestedLoop_whenInitializingNewScores() = runBlocking {
        // Given: No existing scores for rider
        val riderId = 1
        val numSections = 10
        val requestedLoop = 2

        val dao = RiderScoreDaoFake()
        val sut = SectionScoreRepository(dao)

        // When: Fetching scores for loop 2 (will trigger initialization)
        val result = sut.fetchOrInitRiderScore(riderId, requestedLoop, numSections).first()

        // Then: Should return only loop 2 scores (10 sections), not all loops
        assertThat(result.sectionScores, hasSize(numSections))
        assertThat(result.getLoopNumber(), equalTo(requestedLoop))
    }

    class RiderScoreDaoFake : RiderScoreDao {
        val existingScores = mutableListOf<SectionScore>()

        override fun getAll(): Flow<List<RiderScoreAggregate>> {
            TODO("Not yet implemented")
        }

        override fun getRider(riderId: Int): Flow<RiderScore> {
            TODO("Not yet implemented")
        }

        override fun sectionScores(riderId: Int, loopNumber: Int): Flow<List<SectionScore>> = flow {
            // Return only scores for the requested loop
            val scores = existingScores.filter { it.riderId == riderId && it.loopNumber == loopNumber }
            emit(scores)
        }

        override suspend fun updateSectionScore(sectionScore: SectionScore) {
            TODO("Not yet implemented")
        }

        override suspend fun deleteAllRiders() {
            TODO("Not yet implemented")
        }

        override suspend fun deleteAllScores() {
            TODO("Not yet implemented")
        }

        override suspend fun deleteRiderScores(riderId: Int) {
            TODO("Not yet implemented")
        }

        override suspend fun insertAll(sectionScores: List<SectionScore>) {
            existingScores.addAll(sectionScores)
        }

        override suspend fun addRider(riderScore: RiderScore) {
            TODO("Not yet implemented")
        }

        override fun fetchSummary(): Flow<List<RiderScoreSummary>> {
            TODO("Not yet implemented")
        }

        override suspend fun updateRider(riderScore: RiderScore) {
            TODO("Not yet implemented")
        }
    }
}
