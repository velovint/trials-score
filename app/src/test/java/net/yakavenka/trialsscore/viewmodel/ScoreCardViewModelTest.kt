package net.yakavenka.trialsscore.viewmodel

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.SectionScoreRepository
import net.yakavenka.trialsscore.data.UserPreferences
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScoreCardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ScoreCardViewModel
    private lateinit var mockSectionScoreRepository: SectionScoreRepository
    private lateinit var mockUserPreferencesRepository: UserPreferencesRepository

    private val testRiderId = 42
    private val testLoopNumber = 2

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSectionScoreRepository = mock(SectionScoreRepository::class.java)
        mockUserPreferencesRepository = mock(UserPreferencesRepository::class.java)

        // Mock the userPreferencesFlow with a test flow
        val prefsFlow = MutableStateFlow(
            UserPreferences(
                numSections = 10,
                numLoops = 3,
                riderClasses = setOf("Expert", "Intermediate")
            )
        )
        `when`(mockUserPreferencesRepository.userPreferencesFlow).thenReturn(prefsFlow)

        // Mock the repository methods that are called in the ViewModel constructor
        `when`(mockSectionScoreRepository.fetchOrInitRiderScore(any(), any(), any()))
            .thenReturn(emptyFlow())

        `when`(mockSectionScoreRepository.getRiderInfo(any()))
            .thenReturn(emptyFlow())

        val savedStateHandle = SavedStateHandle().apply {
            set("riderId", testRiderId)
            set("loop", testLoopNumber)
        }

        viewModel = ScoreCardViewModel(
            sectionScoreRepository = mockSectionScoreRepository,
            userPreferencesRepository = mockUserPreferencesRepository,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateSectionScore_callsRepository() = runTest {
        // Setup
        val score = SectionScore(testRiderId, testLoopNumber, 1, 0)

        // Action
        viewModel.updateSectionScore(score)
        advanceUntilIdle()

        // Assert: Repository should be called with the score
        verify(mockSectionScoreRepository).updateSectionScore(score)
    }

    @Test
    fun clearScores_callsRepository() = runTest {
        // Action
        viewModel.clearScores(testRiderId)
        advanceUntilIdle()

        // Assert: Repository should be called to clear scores for this rider
        verify(mockSectionScoreRepository).deleteRiderScores(testRiderId)
    }
}
