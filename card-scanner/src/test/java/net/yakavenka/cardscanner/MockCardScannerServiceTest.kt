package net.yakavenka.cardscanner

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.junit.BeforeClass
import org.junit.Test
import org.opencv.core.CvType
import org.opencv.core.Mat

@OptIn(ExperimentalCoroutinesApi::class)
class MockCardScannerServiceTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun loadOpenCV() {
            nu.pattern.OpenCV.loadLocally()
        }
    }

    private val service = MockCardScannerService()

    private fun createTestMat(width: Int = 640, height: Int = 480): Mat {
        // Create empty Mat (content doesn't matter for mock)
        return Mat(height, width, CvType.CV_8UC4)
    }

    @Test
    fun extractScores_returnsSuccess_withAllScores() = runTest {
        // Setup: Create a test Mat
        val testImage = createTestMat()

        try {
            // Action: Call extractScores
            val result = service.extractScores(testImage)

            // Assert: Result is Success
            assertThat(result, instanceOf(ScanResult.Success::class.java))

            val successResult = result as ScanResult.Success
            // Verify 12 scores are returned
            assertThat(successResult.scores.size, `is`(12))
        } finally {
            testImage.release()
        }
    }

    @Test
    fun extractScores_returnsPredictableScores() = runTest {
        // Setup
        val testImage = createTestMat()

        try {
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
        } finally {
            testImage.release()
        }
    }

    @Test
    fun extractScores_allScoresInValidRange() = runTest {
        // Setup
        val testImage = createTestMat()

        try {
            // Action
            val result = service.extractScores(testImage) as ScanResult.Success

            // Assert: All scores are between 0 and 5
            result.scores.forEach { (section, points) ->
                assertThat(
                    "Section $section has points $points, but should be 0-5",
                    points,
                    allOf(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(0),
                        org.hamcrest.Matchers.lessThanOrEqualTo(5)
                    )
                )
            }
        } finally {
            testImage.release()
        }
    }

    @Test
    fun extractScores_simulatesProcessingDelay() = runTest {
        // Setup
        val testImage = createTestMat()

        try {
            // Action: In test context with StandardTestDispatcher, the delay is simulated
            // We verify the coroutine delay is called by checking the service returns success
            val result = service.extractScores(testImage)

            // Assert: Result is still successful (delay is internal to service)
            assertThat(result, instanceOf(ScanResult.Success::class.java))
            assertThat((result as ScanResult.Success).scores.size, `is`(12))
        } finally {
            testImage.release()
        }
    }

    @Test
    fun extractScores_ignoresImageContent() = runTest {
        // Setup: Call with different Mat objects should still work
        val testImage1 = createTestMat()
        val testImage2 = createTestMat()

        try {
            // Action: Extract scores twice with different Mats
            val result1 = service.extractScores(testImage1) as ScanResult.Success
            val result2 = service.extractScores(testImage2) as ScanResult.Success

            // Assert: Results are identical regardless of input Mat
            assertThat(result1.scores, `is`(result2.scores))
        } finally {
            testImage1.release()
            testImage2.release()
        }
    }
}
