package net.yakavenka.cardscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class OpenCVCardPreprocessorTest {

    // A small fake Bitmap for testing.
    // All test stubs ignore the Bitmap argument entirely, so its contents don't matter.
    private lateinit var fakeImage: Bitmap
    private val templateCache = mutableMapOf<String, Mat>()

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
    // Integration tests: full preprocessing pipeline
    // (Parameterized tests moved to OpenCVCardPreprocessorParameterizedTest)
    // ---------------------------------------------------------------------------

    @Test
    fun preprocess_firstRow_containsSectionNumber1() {
        verifyRowMatchesTemplate(
            rowSelector = { rows -> rows.first() },
            templateName = "template_section_1.png",
            errorMessage = "Row 1 must match section-1 template"
        )
    }

    @Test
    fun preprocess_lastRow_containsSectionNumber15() {
        verifyRowMatchesTemplate(
            rowSelector = { rows -> rows.last() },
            templateName = "template_section_15.png",
            errorMessage = "Row 15 must match section-15 template"
        )
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

    private fun loadBitmapFromAssets(filename: String): Bitmap {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return bitmap!!
    }

    private fun loadTemplateFromAssets(filename: String): Mat {
        // Cache templates since they're reused across tests
        return templateCache.getOrPut(filename) {
            loadGrayscaleFromAssets(filename, cache = true)
        }
    }

    private fun loadGrayscaleFromAssets(filename: String, cache: Boolean): Mat {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        mat.release()

        return gray
    }

    private fun rowImageToMat(row: RowImage): Mat {
        val mat = Mat(66, 640, CvType.CV_32F)
        row.buffer.rewind()
        val floatArray = FloatArray(66 * 640)
        row.buffer.asFloatBuffer().get(floatArray)
        mat.put(0, 0, floatArray)
        return mat
    }

    private fun verifyRowMatchesTemplate(
        rowSelector: (List<RowImage>) -> RowImage,
        templateName: String,
        errorMessage: String
    ) {
        val bitmap = loadBitmapFromAssets("test_score_card_grid_gap.png")
        val rows = OpenCVCardPreprocessor(FileScanDebugObserver(TestStorage(), InstrumentationRegistry.getInstrumentation().targetContext)).preprocess(bitmap).getOrThrow()

        val row = rowSelector(rows)
        val rowMatConverted = convertRowToTemplateFormat(row)
        val template = loadTemplateFromAssets(templateName)
        val result = Mat()

        Imgproc.matchTemplate(rowMatConverted, template, result, Imgproc.TM_CCOEFF_NORMED)
        val minMax = Core.minMaxLoc(result)

        assertThat(errorMessage, minMax.maxVal, greaterThanOrEqualTo(0.7))

        // Cleanup
        rowMatConverted.release()
        template.release()
        result.release()
    }

    private fun convertRowToTemplateFormat(row: RowImage): Mat {
        val rowMat = rowImageToMat(row)
        val rowMatConverted = Mat()
        // Convert float [0, 1] to uint8 [0, 255] for template matching
        Core.multiply(rowMat, Scalar(255.0), rowMatConverted)
        rowMatConverted.convertTo(rowMatConverted, CvType.CV_8U)
        rowMat.release()
        return rowMatConverted
    }
}
