package net.yakavenka.trialsscore.viewmodel

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.yakavenka.trialsscore.data.FakeFileStorage
import net.yakavenka.trialsscore.data.FakeRiderScoreDao
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
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EventScoreViewModelTest {
    private val fakeFileStorage = FakeFileStorage()
    private val fakeDao = FakeRiderScoreDao()
    private lateinit var viewModel: EventScoreViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher) // For ModelView
        val sectionScoreRepository = SectionScoreRepository(fakeDao)
        val scoreSummaryRepository = ScoreSummaryRepository(fakeDao)
        val csvRepository = CsvExchangeRepository(fakeFileStorage, testDispatcher)
        val preferencesRepository = UserPreferencesRepository(FakeDataStore())

        viewModel = EventScoreViewModel(
            scoreSummaryRepository = scoreSummaryRepository,
            sectionScoreRepository = sectionScoreRepository,
            importExportService = csvRepository,
            preferencesRepository = preferencesRepository
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exportReport_writesOnce_whenFlowEmitsMultipleTimes() = runTest {
        // Given: DAO configured to emit twice (simulating reactive Room Flow behavior)
        val testData = listOf(createTestRiderAggregate())
        fakeDao.getAllFlow = flowOf(testData, testData)
        val testUri = Uri.parse("file://test.csv")

        // When: Export is triggered
        viewModel.exportReport(testUri)

        // Then: Export should be called only ONCE (not twice)
        val writtenContent = fakeFileStorage.readStringFromUri(testUri)
        assertThat(
            "CSV header should appear exactly once",
            writtenContent.occurrencesOf("Name,Class"),
            equalTo(1)
        )
    }

    @Test
    fun exportReport_setsSuccessMessage_onSuccess() = runTest {
        // Given
        val testData = listOf(createTestRiderAggregate())
        fakeDao.getAllFlow = flowOf(testData)
        val testUri = Uri.parse("file://test.csv")

        // When
        viewModel.exportReport(testUri)

        // Then
        assertThat(viewModel.snackbarMessage.value, equalTo("Export complete"))
    }

    @Test
    fun exportReport_setsErrorMessage_onException() = runTest {
        // Given: DAO that throws exception
        fakeDao.getAllFlow = flow { throw IllegalStateException("Database error") }
        val testUri = Uri.parse("file://test.csv")

        // When
        viewModel.exportReport(testUri)

        // Then
        val message = viewModel.snackbarMessage.value
        assertThat(message, notNullValue())
        assertThat(message, containsString("Export failed"))
        assertThat(message, containsString("Database error"))
    }

    @Test
    fun clearSnackbarMessage_clearsMessage() = runTest {
        // Given: Message is set
        val testData = listOf(createTestRiderAggregate())
        fakeDao.getAllFlow = flowOf(testData)
        viewModel.exportReport(Uri.parse("file://test.csv"))
        assertThat(viewModel.snackbarMessage.value, notNullValue())

        // When
        viewModel.clearSnackbarMessage()

        // Then
        assertThat(viewModel.snackbarMessage.value, equalTo(null))
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

/**
 * Extension function to count occurrences of a substring.
 * More efficient than regex for simple substring counting.
 */
fun String.occurrencesOf(substring: String): Int =
    split(substring).size - 1
