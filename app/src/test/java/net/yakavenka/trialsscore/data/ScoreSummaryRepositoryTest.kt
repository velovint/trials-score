package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.not
import org.junit.Test

class ScoreSummaryRepositoryTest {
    private val dao: RiderScoreDaoFake = RiderScoreDaoFake()

    private val sut = ScoreSummaryRepository(dao)

    @Test
    fun fetchSummaryReturnsAList() = runBlocking {
        dao.summary.add(RiderScoreSummary(1, "Rider", "Novice", 10, 5, 8))

        val actual = sut.fetchSummary().first()
        assertThat(actual, not(empty()))
    }

    class RiderScoreDaoFake : RiderScoreDao {
        val summary = mutableListOf<RiderScoreSummary>()

        override fun getAll(): Flow<List<RiderScoreAggregate>> {
            TODO("Not yet implemented")
        }

        override fun getRider(riderId: Int): Flow<RiderScore> {
            TODO("Not yet implemented")
        }

        override fun sectionScores(riderId: Int): Flow<List<SectionScore>> {
            TODO("Not yet implemented")
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
            TODO("Not yet implemented")
        }

        override suspend fun addRider(riderScore: RiderScore) {
            TODO("Not yet implemented")
        }

        override fun fetchSummary(): Flow<List<RiderScoreSummary>> = flow {
            emit(summary)
        }

        override suspend fun updateRider(riderScore: RiderScore) {
            TODO("Not yet implemented")
        }

    }
}