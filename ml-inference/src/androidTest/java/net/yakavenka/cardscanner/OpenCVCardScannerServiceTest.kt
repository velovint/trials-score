package net.yakavenka.cardscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
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
        val testImage = loadTestImageFromAssets("raw/PXL_3302032333999999.jpg")

        val result = scanner.extractScores(testImage)
        Log.d("net.yakavenka.test", "Image scanned successfully: $result")

        // Assert result is ScanResult.Success
        assertThat(result, instanceOf(ScanResult.Success::class.java))

        val successResult = result as ScanResult.Success
        // TODO model falls back to 3s for unpunched sections. need to fix at some point
        val expectedScores = listOf(1, 0, 0, 1, 1, 2, 0, 1, 0, 2, 3, 3, 3, 3, 3)
        val actualScores = successResult.scores.map { it.value }

        assertThat(actualScores, `is`(expectedScores))
    }

    @Test
    fun extractScores_withEmptyImage_returnsFailure() = runTest {
        val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val result = scanner.extractScores(emptyBitmap)

        assertThat(result, instanceOf(ScanResult.Failure::class.java))
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
