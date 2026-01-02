package net.yakavenka.trialsscore.viewmodel

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.yakavenka.trialsscore.data.FakeFileStorage
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.ScoreSummaryRepository
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import net.yakavenka.trialsscore.exchange.CsvExchangeRepository
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventScoreViewModelTest {
    private val fakeFileStorage = FakeFileStorage()
    private val fakeDao = FakeRiderScoreDao()
    private lateinit var viewModel: EventScoreViewModel

    @Before
    fun setup() {
        val sectionScoreRepository = SectionScoreRepository(fakeDao)
        val scoreSummaryRepository = ScoreSummaryRepository(fakeDao)
        val csvRepository = CsvExchangeRepository(fakeFileStorage)
        val preferencesRepository = UserPreferencesRepository(FakeDataStore())

        viewModel = EventScoreViewModel(
            scoreSummaryRepository = scoreSummaryRepository,
            sectionScoreRepository = sectionScoreRepository,
            importExportService = csvRepository,
            preferencesRepository = preferencesRepository
        )
    }

    @Test
    fun exportReport_writesOnce_whenFlowEmitsMultipleTimes() = runBlocking {
        // Given: DAO configured to emit twice (simulating reactive Room Flow behavior)
        val testData = listOf(createTestRiderAggregate())
        fakeDao.getAllFlow = flowOf(testData, testData)
        val testUri = Uri.parse("file://test.csv")

        // When: Export is triggered
        viewModel.exportReport(testUri)
        // TODO figure out how to avoid delay
        delay(2000) // Wait for coroutine to complete (increased delay)

        // Then: Export should be called only ONCE (not twice)
        // FakeFileStorage appends on each write, so if called twice, content is duplicated
        val writtenContent = fakeFileStorage.readStringFromUri(testUri)
        val headerOccurrences = "Name,Class".toRegex().findAll(writtenContent).count()

        // Expected: 1 occurrence (single write)
        // With bug: 2 occurrences (content written twice)
        assertThat("CSV header should appear once, but appeared $headerOccurrences times",
                   headerOccurrences, equalTo(1))
    }
}

/**
 * Configurable fake DAO for testing.
 * Tests can set the Flow that should be returned by each method.
 */
class FakeRiderScoreDao : RiderScoreDao {
    /**
     * Configurable Flow for getAll(). Tests should set this to control behavior.
     */
    var getAllFlow: Flow<List<RiderScoreAggregate>> = flow { emit(emptyList()) }

    /**
     * Configurable Flow for fetchSummary(). Defaults to empty list.
     */
    var fetchSummaryFlow: Flow<List<RiderScoreSummary>> = flow { emit(emptyList()) }

    override fun getAll(): Flow<List<RiderScoreAggregate>> = getAllFlow

    override fun fetchSummary(): Flow<List<RiderScoreSummary>> = fetchSummaryFlow

    // Other methods throw NotImplementedError - implement only when needed
    override fun getRider(riderId: Int): Flow<RiderScore> {
        throw NotImplementedError("Not configured for this test")
    }

    override fun sectionScores(riderId: Int, loopNumber: Int): Flow<List<SectionScore>> {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun updateSectionScore(sectionScore: SectionScore) {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun deleteAllRiders() {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun deleteAllScores() {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun deleteRiderScores(riderId: Int) {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun insertAll(sectionScores: List<SectionScore>) {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun addRider(riderScore: RiderScore) {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun updateRider(riderScore: RiderScore) {
        throw NotImplementedError("Not configured for this test")
    }
}

class FakeDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow {
        emit(androidx.datastore.preferences.core.emptyPreferences())
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return androidx.datastore.preferences.core.emptyPreferences()
    }
}

/**
 * Test data factory - creates sample RiderScoreAggregate for testing
 */
fun createTestRiderAggregate(
    riderId: Int = 1,
    riderName: String = "Test Rider",
    riderClass: String = "Expert",
    sectionScores: List<SectionScore> = listOf(
        SectionScore(riderId = 1, loopNumber = 1, sectionNumber = 1, points = 0)
    )
): RiderScoreAggregate {
    val rider = RiderScore(riderId, riderName, riderClass)
    return RiderScoreAggregate(rider, sectionScores)
}
