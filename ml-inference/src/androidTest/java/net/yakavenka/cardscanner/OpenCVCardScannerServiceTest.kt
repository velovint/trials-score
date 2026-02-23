package net.yakavenka.cardscanner

import android.content.Context
import android.graphics.Bitmap
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

@RunWith(AndroidJUnit4::class)
class OpenCVCardScannerServiceTest {

    private lateinit var scanner: OpenCVCardScannerService

    @Before
    fun setup() {
        // Load OpenCV native library
        OpenCVLoader.initLocal()

        val context = ApplicationProvider.getApplicationContext<Context>()
        scanner = OpenCVCardScannerService(context)
    }

    @Test
    fun extractScores_withRealScoreCard_returnsValidScores() = runTest {
        // Load actual score card image from test assets
        val testImage = loadTestImageFromAssets("raw/PXL_100112010299999.jpg")

        val result = scanner.extractScores(testImage)

        // Assert result is ScanResult.Success
        assertThat(result, instanceOf(ScanResult.Success::class.java))

        // Assert 15 rows extracted
        val successResult = result as ScanResult.Success
        assertThat(successResult.scores.size, `is`(15))

        // Assert all scores are valid (0, 1, 2, 3, or 5)
        val validScores = setOf(0, 1, 2, 3, 5)
        for (score in successResult.scores) {
            assertThat(score, isIn(validScores))
        }
    }

    @Test
    fun extractScores_withEmptyImage_returnsFailure() = runTest {
        val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val result = scanner.extractScores(emptyBitmap)

        assertThat(result, instanceOf(ScanResult.Failure::class.java))
    }

    @After
    fun teardown() {
        scanner.cleanup()
    }

    /**
     * Load test image from androidTest/assets and return as Bitmap.
     */
    private fun loadTestImageFromAssets(filename: String): Bitmap {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)

        // Decode bitmap from input stream
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        return bitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}
