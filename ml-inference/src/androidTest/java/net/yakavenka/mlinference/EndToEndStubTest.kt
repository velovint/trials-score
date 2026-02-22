package net.yakavenka.mlinference

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.yakavenka.cardscanner.CardScanningPipeline
import net.yakavenka.cardscanner.MorphologicalRowSegmenter
import net.yakavenka.cardscanner.OpenCVCardIsolator
import net.yakavenka.cardscanner.OpenCVRowNormalizer
import net.yakavenka.cardscanner.ScanResult
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Slice 3.3: End-to-end stub pipeline on a real card image.
 *
 * Runs CardScanningPipeline (stubs 2.1–2.4) on a real score card image from assets
 * and asserts that exactly 15 scores are returned, all equal to 0.
 *
 * Pipeline composition:
 *   - OpenCVCardIsolator  — passthrough stub (Slice 2.1)
 *   - MorphologicalRowSegmenter — 15 equal-height rows stub (Slice 2.2)
 *   - OpenCVRowNormalizer — full implementation (Slice 2.3)
 *   - TFLiteRowClassifier — always returns 0 stub (Slice 2.4)
 */
@RunWith(AndroidJUnit4::class)
class EndToEndStubTest {

    @Before
    fun setUp() {
        OpenCVLoader.initLocal()
    }

    @Test
    fun scan_onRealCardImage_returns15ScoresAllZero() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val colorMat = Mat()
        Utils.bitmapToMat(bitmap, colorMat)
        val grayMat = Mat()
        Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        colorMat.release()

        val pipeline = CardScanningPipeline(
            isolator   = OpenCVCardIsolator(),
            segmenter  = MorphologicalRowSegmenter(),
            normalizer = OpenCVRowNormalizer(),
            classifier = TFLiteRowClassifier(context),
        )

        val result = pipeline.scan(grayMat)
        grayMat.release()

        assertThat("Pipeline should succeed", result.isSuccess, equalTo(true))
        val scanResult = result.getOrThrow()
        assertThat("Result should be ScanResult.Success", scanResult is ScanResult.Success, equalTo(true))
        val scores = (scanResult as ScanResult.Success).scores
        assertThat("Should return exactly 15 scores", scores.size, equalTo(15))
        scores.forEach { (section, score) ->
            assertThat("Score for section $section should be 0", score, equalTo(0))
        }
    }
}
