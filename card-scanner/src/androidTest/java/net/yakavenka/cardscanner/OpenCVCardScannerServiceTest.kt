package net.yakavenka.cardscanner

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

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
    fun extractScores_withRealScoreCard_returnsValidScores() = runTest {
        // Load actual score card image from test assets
        val testImage = loadTestImageFromAssets("test_score_card.jpg")

        val result = scanner.extractScores(testImage)

        assertThat(result, instanceOf(ScanResult.Success::class.java))
        val scores = (result as ScanResult.Success).scores
        assertThat(scores.size, equalTo(15))

        // Verify all scores are valid
        scores.values.forEach { score ->
            assertThat(score, isIn(listOf(0, 1, 2, 3, 5)))
        }

        // Log the detected scores for debugging
        println("Detected scores from real image: $scores")

        testImage.release()
    }

    @Test
    fun extractScores_withEmptyImage_returnsFailure() = runTest {
        val emptyMat = Mat()

        val result = scanner.extractScores(emptyMat)

        assertThat(result, instanceOf(ScanResult.Failure::class.java))

        emptyMat.release()
    }

    @After
    fun teardown() {
        scanner.cleanup()
    }

    /**
     * Load test image from androidTest/assets and convert to grayscale Mat.
     */
    private fun loadTestImageFromAssets(filename: String): Mat {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)

        // Decode bitmap from input stream
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert bitmap to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        return gray
    }

    private fun createTestMat(width: Int, height: Int): Mat {
        // Create grayscale Mat (CV_8UC1)
        return Mat(height, width, CvType.CV_8UC1)
    }
}
