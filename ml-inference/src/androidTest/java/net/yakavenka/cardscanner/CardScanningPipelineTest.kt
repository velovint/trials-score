package net.yakavenka.cardscanner

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
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
class CardScanningPipelineTest {

    // A small fake Bitmap for testing.
    // All test stubs ignore the Bitmap argument entirely, so its contents don't matter.
    private lateinit var fakeImage: Bitmap

    @Before
    fun setUp() {
        OpenCVLoader.initLocal()
        fakeImage = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
    }

    // ---------------------------------------------------------------------------
    // Happy path: all rows classified as 3, 15 rows returned
    // ---------------------------------------------------------------------------

    @Test
    fun scan_returnsSuccess_whenAllStepsSucceed() {
        val result = happyPathPipeline().scan(fakeImage)

        assertTrue("Expected Result.success but got failure", result.isSuccess)
    }

    @Test
    fun scan_returnsSuccessWithCorrectType_whenAllStepsSucceed() {
        val scanResult = happyPathPipeline().scan(fakeImage).getOrNull()

        assertThat(scanResult, instanceOf(ScanResult.Success::class.java))
    }

    @Test
    fun scan_returns15Scores_whenAllStepsSucceed() {
        val scores = (happyPathPipeline().scan(fakeImage).getOrThrow() as ScanResult.Success).scores

        assertThat(scores.size, equalTo(15))
    }

    @Test
    fun scan_returnsAllThrees_whenClassifierAlwaysReturnsThree() {
        val scores = (happyPathPipeline().scan(fakeImage).getOrThrow() as ScanResult.Success).scores

        assertTrue(
            "Expected all scores to equal 3",
            scores.values.all { it == 3 }
        )
    }

    @Test
    fun scan_usesSectionKeys1To15_whenClassifierAlwaysReturnsThree() {
        val scores = (happyPathPipeline().scan(fakeImage).getOrThrow() as ScanResult.Success).scores

        assertThat(scores.keys.sorted(), equalTo((1..15).toList()))
    }

    // ---------------------------------------------------------------------------
    // Failure propagation: CardIsolator failure short-circuits the pipeline
    // ---------------------------------------------------------------------------

    @Test
    fun scan_returnsFailure_whenIsolatorFails() {
        val result = failingIsolatorPipeline().scan(fakeImage)

        assertFalse("Expected Result.failure but got success", result.isSuccess)
    }

    @Test
    fun scan_doesNotCallDownstreamSteps_whenIsolatorFails() {
        var segmenterCalled = false
        var normalizerCalled = false
        var classifierCalled = false

        val pipeline = CardScanningPipeline(
            isolator   = { _ -> Result.failure(RuntimeException("Card not detected")) },
            segmenter  = { _ ->
                segmenterCalled = true
                Result.success(emptyList())
            },
            normalizer = { _, _ ->
                normalizerCalled = true
                emptyList()
            },
            classifier = { _ ->
                classifierCalled = true
                0
            },
        )

        pipeline.scan(fakeImage)

        assertFalse("Segmenter should not have been called", segmenterCalled)
        assertFalse("Normalizer should not have been called", normalizerCalled)
        assertFalse("Classifier should not have been called", classifierCalled)
    }

    @Test
    fun scan_propagatesOriginalException_whenIsolatorFails() {
        val isolatorError = RuntimeException("Card not detected")

        val pipeline = CardScanningPipeline(
            isolator   = { _ -> Result.failure(isolatorError) },
            segmenter  = { _ -> Result.success(emptyList()) },
            normalizer = { _, _ -> emptyList() },
            classifier = { _ -> 0 },
        )

        val result = pipeline.scan(fakeImage)

        assertThat(result.exceptionOrNull(), equalTo(isolatorError))
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun happyPathPipeline() = CardScanningPipeline(
        isolator   = { _ -> Result.success(Mat(10, 10, CvType.CV_8UC1)) },
        segmenter  = { _ ->
            Result.success(List(15) { i -> RowRegion(i * 10, (i + 1) * 10) })
        },
        normalizer = { _, _ ->
            List(15) { RowImage(ByteBuffer.allocateDirect(66 * 640 * 4)) }
        },
        classifier = { _ -> 3 },
    )

    private fun failingIsolatorPipeline() = CardScanningPipeline(
        isolator   = { _ -> Result.failure(RuntimeException("Card not detected")) },
        segmenter  = { _ -> Result.success(emptyList()) },
        normalizer = { _, _ -> emptyList() },
        classifier = { _ -> 0 },
    )
}
