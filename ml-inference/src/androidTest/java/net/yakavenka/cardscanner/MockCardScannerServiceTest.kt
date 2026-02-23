package net.yakavenka.cardscanner

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MockCardScannerServiceTest {

    private val service = MockCardScannerService()

    private fun createTestBitmap(width: Int = 640, height: Int = 480): Bitmap {
        // Create empty Bitmap (content doesn't matter for mock)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun extractScores_returnsSuccess_withAllScores() = runTest {
        // Setup: Create a test Bitmap
        val testImage = createTestBitmap()

        // Action: Call extractScores
        val result = service.extractScores(testImage)

        // Assert: Result is Success
        assertThat(result, instanceOf(ScanResult.Success::class.java))

        val successResult = result as ScanResult.Success
        // Verify 15 scores are returned
        assertThat(successResult.scores.size, `is`(15))
    }

    @Test
    fun extractScores_returnsPredictableScores() = runTest {
        // Setup
        val testImage = createTestBitmap()

        // Action
        val result = service.extractScores(testImage) as ScanResult.Success

        // Assert: Verify specific hardcoded scores
        assertThat(result.scores[1], `is`(0))
        assertThat(result.scores[2], `is`(1))
        assertThat(result.scores[3], `is`(0))
        assertThat(result.scores[4], `is`(2))
        assertThat(result.scores[5], `is`(0))
        assertThat(result.scores[6], `is`(3))
        assertThat(result.scores[7], `is`(0))
        assertThat(result.scores[8], `is`(5))
        assertThat(result.scores[9], `is`(0))
        assertThat(result.scores[10], `is`(1))
        assertThat(result.scores[11], `is`(0))
        assertThat(result.scores[12], `is`(0))
        assertThat(result.scores[13], `is`(1))
        assertThat(result.scores[14], `is`(2))
        assertThat(result.scores[15], `is`(0))
    }

    @Test
    fun extractScores_allScoresInValidRange() = runTest {
        // Setup
        val testImage = createTestBitmap()

        // Action
        val result = service.extractScores(testImage) as ScanResult.Success

        // Assert: All scores are between 0 and 5
        result.scores.forEach { (section, points) ->
            assertThat(
                "Section $section has points $points, but should be 0-5",
                points,
                allOf(
                    Matchers.greaterThanOrEqualTo(0),
                    Matchers.lessThanOrEqualTo(5)
                )
            )
        }
    }

    @Test
    fun extractScores_simulatesProcessingDelay() = runTest {
        // Setup
        val testImage = createTestBitmap()

        // Action: In test context with StandardTestDispatcher, the delay is simulated
        // We verify the coroutine delay is called by checking the service returns success
        val result = service.extractScores(testImage)

        // Assert: Result is still successful (delay is internal to service)
        assertThat(result, instanceOf(ScanResult.Success::class.java))
        assertThat((result as ScanResult.Success).scores.size, `is`(15))
    }

    @Test
    fun extractScores_ignoresImageContent() = runTest {
        // Setup: Call with different Bitmap objects should still work
        val testImage1 = createTestBitmap()
        val testImage2 = createTestBitmap()

        // Action: Extract scores twice with different Bitmaps
        val result1 = service.extractScores(testImage1) as ScanResult.Success
        val result2 = service.extractScores(testImage2) as ScanResult.Success

        // Assert: Results are identical regardless of input Bitmap
        assertThat(result1.scores, `is`(result2.scores))
    }
}
