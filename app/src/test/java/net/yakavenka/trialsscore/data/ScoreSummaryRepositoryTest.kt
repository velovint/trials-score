package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test

class ScoreSummaryRepositoryTest {
    private val dao: RiderScoreDaoFake = RiderScoreDaoFake()

    private val sut = ScoreSummaryRepository(dao)

    @Test
    fun fetchSummaryReturnsAList() = runBlocking {
        dao.summary.add(RiderScoreSummary(1, "Rider", "Novice", SectionScore.Set.TOTAL_SECTIONS, 5, 8))

        val actual = sut.fetchSummary().first()
        assertThat(actual, not(empty()))
    }

    @Test
    fun fetchSummarySortsResultByClass() = runBlocking {
        dao.summary.add(RiderScoreSummary(1, "Rider1", "Novice", SectionScore.Set.TOTAL_SECTIONS, 5, 8))
        dao.summary.add(RiderScoreSummary(2, "Rider2", "Advanced", SectionScore.Set.TOTAL_SECTIONS, 5, 8))
        dao.summary.add(RiderScoreSummary(3, "Rider3", "Novice", SectionScore.Set.TOTAL_SECTIONS, 5, 8))

        val actual: List<String> = sut.fetchSummary().first().map { it.riderClass }

        assertThat("Num groups", numGroups(actual), equalTo(2))
    }

    private fun numGroups(actual: List<String>): Int {
        var result = 0
        var lastValue: String? = null
        for (entry in actual) {
            if (!entry.equals(lastValue)) result ++
            lastValue = entry
        }
        return result
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

    }
}