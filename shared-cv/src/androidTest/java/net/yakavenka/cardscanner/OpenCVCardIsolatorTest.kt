package net.yakavenka.cardscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader

@RunWith(AndroidJUnit4::class)
class OpenCVCardIsolatorTest {

    private lateinit var isolator: OpenCVCardIsolator

    @Before
    fun setup() {
        OpenCVLoader.initLocal()
        isolator = OpenCVCardIsolator()
    }

    @Test
    fun isolate_succeeds_withGrayscaleSidewaysCard() {
        val input = loadBitmapFromAssets("score-card-sideways.jpg")

        val result = isolator.isolate(input)

        assertThat("Expected success but got: ${result.exceptionOrNull()}", result.isSuccess)
        result.getOrNull()?.release()
    }

    @Test
    fun isolate_outputWidthIs640() {
        val input = loadBitmapFromAssets("score-card-sideways.jpg")

        val mat = isolator.isolate(input).getOrThrow()

        assertThat("Output width must be 640px", mat.width(), equalTo(640))
        mat.release()
    }

    @Test
    fun isolate_outputIsPortrait() {
        // Card is sideways in the photo — isolator must rotate to portrait
        val input = loadBitmapFromAssets("score-card-sideways.jpg")

        val mat = isolator.isolate(input).getOrThrow()

        assertThat(
            "Output must be portrait (height > width), got ${mat.width()}x${mat.height()}",
            mat.height(),
            greaterThan(mat.width())
        )
        mat.release()
    }

    @Test
    fun isolate_outputAspectRatioMeetsMinimum() {
        // MIN_PORTRAIT_ASPECT_RATIO = 1.5 is the guard inside detectAndCropCard
        val input = loadBitmapFromAssets("score-card-sideways.jpg")

        val mat = isolator.isolate(input).getOrThrow()
        val ratio = mat.height().toDouble() / mat.width()

        Log.i("OpenCVCardIsolatorTest", "Output aspect ratio: ${"%.2f".format(ratio)}")
        assertThat(
            "Aspect ratio ${"%.2f".format(ratio)} must be >= 1.5",
            ratio,
            greaterThanOrEqualTo(1.5)
        )
        mat.release()
    }

    @Test
    fun isolate_outputIsGrayscale() {
        val input = loadBitmapFromAssets("score-card-sideways.jpg")

        val mat = isolator.isolate(input).getOrThrow()

        assertThat("Output must be single-channel grayscale", mat.channels(), equalTo(1))
        mat.release()
    }

    @Test
    fun isolate_logsOutputDimensions() {
        val input = loadBitmapFromAssets("score-card-sideways.jpg")

        val mat = isolator.isolate(input).getOrThrow()

        Log.i("OpenCVCardIsolatorTest", "Input: ${input.width}x${input.height}")
        Log.i("OpenCVCardIsolatorTest", "Output: ${mat.width()}x${mat.height()}")
        Log.i("OpenCVCardIsolatorTest", "Channels: ${mat.channels()}")
        Log.i("OpenCVCardIsolatorTest", "Aspect ratio: ${"%.2f".format(mat.height().toDouble() / mat.width())}")

        mat.release()
    }

    @Test
    fun isolate_outputWidthIsNatural() {
        val input = loadBitmapFromAssets("score-card-sideways.jpg")
        val mat = OpenCVCardIsolator(targetWidth = null).isolate(input).getOrThrow()
        assertThat("Output must be natural card width (not fixed 640px)",
            mat.width(), greaterThan(800))
        mat.release()
    }

    // ========== Helper Functions ==========

    /**
     * Loads an image as a Bitmap from assets.
     * BitmapFactory produces ARGB_8888 bitmap.
     * The isolator will handle conversion to grayscale internally.
     */
    private fun loadBitmapFromAssets(filename: String): Bitmap {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open(filename).use { BitmapFactory.decodeStream(it) }!!
    }
}
