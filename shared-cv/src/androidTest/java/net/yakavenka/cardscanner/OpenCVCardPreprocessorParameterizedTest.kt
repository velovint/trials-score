package net.yakavenka.cardscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

@RunWith(Parameterized::class)
class OpenCVCardPreprocessorParameterizedTest(
    private val imageFile: String,
    private val rowSelectorName: String,
    private val rowSelector: (List<RowImage>) -> RowImage,
    private val templateName: String,
    private val errorMessage: String
) {
    private val templateCache = mutableMapOf<String, Mat>()

    @Before
    fun setUp() {
        OpenCVLoader.initLocal()
    }

    @Test
    fun verifyRowMatchesTemplate() {
        val bitmap = loadBitmapFromAssets(imageFile)
        val rows = OpenCVCardPreprocessor().preprocess(bitmap).getOrThrow()

        val row = rowSelector(rows)
        val rowMatConverted = convertRowToTemplateFormat(row)
        val template = loadTemplateFromAssets(templateName)
        val result = Mat()

        Imgproc.matchTemplate(rowMatConverted, template, result, Imgproc.TM_CCOEFF_NORMED)
        val minMax = Core.minMaxLoc(result)

        assertThat(errorMessage, minMax.maxVal, greaterThanOrEqualTo(0.55))

        // Cleanup
        rowMatConverted.release()
        template.release()
        result.release()
    }

    private fun loadBitmapFromAssets(filename: String): Bitmap {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return bitmap!!
    }

    private fun loadTemplateFromAssets(filename: String): Mat {
        return templateCache.getOrPut(filename) {
            loadGrayscaleFromAssets(filename)
        }
    }

    private fun loadGrayscaleFromAssets(filename: String): Mat {
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

    private fun convertRowToTemplateFormat(row: RowImage): Mat {
        val rowMat = rowImageToMat(row)
        val rowMatConverted = Mat()
        // Convert float [0, 1] to uint8 [0, 255] for template matching
        Core.multiply(rowMat, Scalar(255.0), rowMatConverted)
        rowMatConverted.convertTo(rowMatConverted, CvType.CV_8U)
        rowMat.release()
        return rowMatConverted
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1} with {0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "test_score_card_w_header.png",
                "firstRow",
                { rows: List<RowImage> -> rows.first() },
                "template_section_1.png",
                "Row 1 must match section-1 template (upright v1)"
            ),
            arrayOf(
                "test_score_card_w_header_1.png",
                "firstRow",
                { rows: List<RowImage> -> rows.first() },
                "template_section_1.png",
                "Row 1 must match section-1 template (upright v2)"
            ),
            arrayOf(
                "score-card-sideways.jpg",
                "firstRow",
                { rows: List<RowImage> -> rows.first() },
                "template_section_1.png",
                "Row 1 must match section-1 template (sideways)"
            ),
            arrayOf(
                "test_score_card_w_header.png",
                "lastRow",
                { rows: List<RowImage> -> rows.last() },
                "template_section_15.png",
                "Row 15 must match section-15 template (upright v1)"
            ),
            arrayOf(
                "test_score_card_w_header_1.png",
                "lastRow",
                { rows: List<RowImage> -> rows.last() },
                "template_section_15.png",
                "Row 15 must match section-15 template (upright v2)"
            ),
            arrayOf(
                "score-card-sideways.jpg",
                "lastRow",
                { rows: List<RowImage> -> rows.last() },
                "template_section_15.png",
                "Row 15 must match section-15 template (sideways)"
            ),
        )
    }
}
