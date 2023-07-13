package net.yakavenka.trialsscore.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test

class ScoreSummaryRepositoryTest {
    private val dao: RiderScoreDaoFake = RiderScoreDaoFake()

    private val sut = ScoreSummaryRepository(dao, UserPreferencesRepository(SharedPreferencesFake()))

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

    @Test
    fun fetchSummarySetsStandingsWithinAClass() = runBlocking {
        dao.summary.add(RiderScoreSummary(1, "Rider1", "Novice", SectionScore.Set.TOTAL_SECTIONS, 5, 8))
        dao.summary.add(RiderScoreSummary(2, "Rider2", "Advanced", SectionScore.Set.TOTAL_SECTIONS, 5, 8))
        dao.summary.add(RiderScoreSummary(3, "Rider3", "Novice", SectionScore.Set.TOTAL_SECTIONS, 2, 8))

        val actual = sut.fetchSummary().first()

        assertThat("Top novice", actual[1].riderName, equalTo("Rider3"))
        assertThat("Top place", actual[1].standing, equalTo(1))
        assertThat("Second novice", actual[2].standing, equalTo(2))
    }

    private fun numGroups(actual: List<String>): Int {
        var result = 0
        var lastValue: String? = null
        for (entry in actual) {
            if (entry != lastValue) result ++
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

        override suspend fun updateRider(riderScore: RiderScore) {
            TODO("Not yet implemented")
        }

    }

    class SharedPreferencesFake : SharedPreferences {
        override fun getAll(): MutableMap<String, *> {
            TODO("Not yet implemented")
        }

        override fun getString(key: String?, defValue: String?): String? {
            if (key == UserPreferencesRepository.NUM_SECTIONS_KEY) return "30"
            return null
        }

        override fun getStringSet(
            key: String?,
            defValues: MutableSet<String>?
        ): MutableSet<String>? {
            TODO("Not yet implemented")
        }

        override fun getInt(key: String?, defValue: Int): Int {
            TODO("Not yet implemented")
        }

        override fun getLong(key: String?, defValue: Long): Long {
            TODO("Not yet implemented")
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            TODO("Not yet implemented")
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            TODO("Not yet implemented")
        }

        override fun contains(key: String?): Boolean {
            TODO("Not yet implemented")
        }

        override fun edit(): SharedPreferences.Editor {
            TODO("Not yet implemented")
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("Not yet implemented")
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            TODO("Not yet implemented")
        }

    }
}