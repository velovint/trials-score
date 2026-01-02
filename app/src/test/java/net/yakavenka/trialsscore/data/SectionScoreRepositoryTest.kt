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

        val dao = FakeRiderScoreDao()
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

        val dao = FakeRiderScoreDao()
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

        val dao = FakeRiderScoreDao()
        val sut = SectionScoreRepository(dao)

        // When: Fetching scores for loop 2 (will trigger initialization)
        val result = sut.fetchOrInitRiderScore(riderId, requestedLoop, numSections).first()

        // Then: Should return only loop 2 scores (10 sections), not all loops
        assertThat(result.sectionScores, hasSize(numSections))
        assertThat(result.getLoopNumber(), equalTo(requestedLoop))
    }

}
