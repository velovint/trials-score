package net.yakavenka.cardscanner

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class OpenCVCardPreprocessorTest {

    // A small fake Bitmap for testing.
    // All test stubs ignore the Bitmap argument entirely, so its contents don't matter.
    private lateinit var fakeImage: Bitmap

    @Before
    fun setUp() {
        OpenCVLoader.initLocal()
        fakeImage = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
    }

    // ---------------------------------------------------------------------------
    // Happy path: all steps succeed, 15 rows returned
    // ---------------------------------------------------------------------------

    @Test
    fun preprocess_returnsSuccess_whenAllStepsSucceed() {
        val result = happyPathPreprocessor().preprocess(fakeImage)

        assertTrue("Expected Result.success but got failure", result.isSuccess)
    }

    @Test
    fun preprocess_returns15Rows_whenAllStepsSucceed() {
        val rows = happyPathPreprocessor().preprocess(fakeImage).getOrNull()

        assertThat("Should return exactly 15 rows", rows?.size, equalTo(15))
    }

    // ---------------------------------------------------------------------------
    // Failure propagation: CardIsolator failure short-circuits the pipeline
    // ---------------------------------------------------------------------------

    @Test
    fun preprocess_returnsFailure_whenIsolatorFails() {
        val result = failingIsolatorPreprocessor().preprocess(fakeImage)

        assertFalse("Expected Result.failure but got success", result.isSuccess)
    }

    @Test
    fun preprocess_doesNotCallSegmenterOrNormalizer_whenIsolatorFails() {
        var segmenterCalled = false
        var normalizerCalled = false

        val preprocessor = OpenCVCardPreprocessor(
            isolator   = { _ -> Result.failure(RuntimeException("Card not detected")) },
            segmenter  = { _ ->
                segmenterCalled = true
                Result.success(emptyList())
            },
            normalizer = { _, _ ->
                normalizerCalled = true
                emptyList()
            },
        )

        preprocessor.preprocess(fakeImage)

        assertFalse("Segmenter should not have been called", segmenterCalled)
        assertFalse("Normalizer should not have been called", normalizerCalled)
    }

    @Test
    fun preprocess_propagatesOriginalException_whenIsolatorFails() {
        val isolatorError = RuntimeException("Card not detected")

        val preprocessor = OpenCVCardPreprocessor(
            isolator   = { _ -> Result.failure(isolatorError) },
            segmenter  = { _ -> Result.success(emptyList()) },
            normalizer = { _, _ -> emptyList() },
        )

        val result = preprocessor.preprocess(fakeImage)

        assertThat(result.exceptionOrNull(), equalTo(isolatorError))
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun happyPathPreprocessor() = OpenCVCardPreprocessor(
        isolator   = { _ -> Result.success(Mat(10, 10, CvType.CV_8UC1)) },
        segmenter  = { _ ->
            Result.success(List(15) { i -> RowRegion(i * 10, (i + 1) * 10) })
        },
        normalizer = { _, _ ->
            List(15) { RowImage(ByteBuffer.allocateDirect(66 * 640 * 4)) }
        },
    )

    private fun failingIsolatorPreprocessor() = OpenCVCardPreprocessor(
        isolator   = { _ -> Result.failure(RuntimeException("Card not detected")) },
        segmenter  = { _ -> Result.success(emptyList()) },
        normalizer = { _, _ -> emptyList() },
    )
}
