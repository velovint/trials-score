package net.yakavenka.cardscanner

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat

@RunWith(AndroidJUnit4::class)
class OpenCVCardScannerServiceTest {

    private lateinit var scanner: OpenCVCardScannerService

    @Before
    fun setup() {
        // Load OpenCV native library
        OpenCVLoader.initLocal()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        scanner = OpenCVCardScannerService(context)
    }

    @Test
    fun extractScores_withTestImage_returnsValidScores() = runTest {
        val testImage = createTestMat(179, 1374)

        val result = scanner.extractScores(testImage)

        assertThat(result, instanceOf(ScanResult.Success::class.java))
        val scores = (result as ScanResult.Success).scores
        assertThat(scores.size, equalTo(15))

        // Verify all scores are valid
        scores.values.forEach { score ->
            assertThat(score, isIn(listOf(0, 1, 2, 3, 5)))
        }

        testImage.release()
    }

    @Test
    fun extractScores_withNullImage_returnsFailure() = runTest {
        val emptyMat = Mat()

        val result = scanner.extractScores(emptyMat)

        assertThat(result, instanceOf(ScanResult.Failure::class.java))
    }

    @After
    fun teardown() {
        scanner.cleanup()
    }

    private fun createTestMat(height: Int, width: Int): Mat {
        return Mat(height, width, CvType.CV_8UC1)
    }
}
