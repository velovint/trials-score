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
    private val dao: FakeRiderScoreDao = FakeRiderScoreDao()

    private val sut = ScoreSummaryRepository(dao)

    @Test
    fun fetchSummaryReturnsAList() = runBlocking {
        dao.summary.add(RiderScoreSummary(1, "Rider", "Novice", 10, 5, 8))

        val actual = sut.fetchSummary().first()
        assertThat(actual, not(empty()))
    }

}