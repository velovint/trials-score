package net.yakavenka.cardscanner

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class OpenCVRowNormalizerTest {

    @Before
    fun setup() {
        OpenCVLoader.initLocal()
    }

    @Test
    fun normalize_bufferHasCorrectSize() {
        val card = loadGrayscaleCardFromAssets("test_score_card_no_header.jpg")
        val region = RowRegion(top = 0, bottom = 50)

        val results = OpenCVRowNormalizer().normalize(card, listOf(region))

        assertThat(results, hasSize(1))
        assertThat(results[0].buffer.capacity(), equalTo(66 * 640 * 4))

        card.release()
    }

    @Test
    fun normalize_bufferHasNativeByteOrder() {
        val card = loadGrayscaleCardFromAssets("test_score_card_no_header.jpg")
        val region = RowRegion(top = 0, bottom = 50)

        val results = OpenCVRowNormalizer().normalize(card, listOf(region))

        assertThat(results[0].buffer.order(), equalTo(ByteOrder.nativeOrder()))

        card.release()
    }

    @Test
    fun normalize_bufferPositionIsZero() {
        val card = loadGrayscaleCardFromAssets("test_score_card_no_header.jpg")
        val region = RowRegion(top = 0, bottom = 50)

        val results = OpenCVRowNormalizer().normalize(card, listOf(region))

        assertThat(results[0].buffer.position(), equalTo(0))

        card.release()
    }

    @Test
    fun normalize_allFloatValuesInUnitRange() {
        val card = loadGrayscaleCardFromAssets("test_score_card_no_header.jpg")
        val region = RowRegion(top = 0, bottom = 50)

        val results = OpenCVRowNormalizer().normalize(card, listOf(region))

        val floatBuffer = results[0].buffer.asFloatBuffer()
        val expectedFloatCount = 66 * 640
        assertThat(floatBuffer.capacity(), equalTo(expectedFloatCount))

        for (i in 0 until floatBuffer.capacity()) {
            val value = floatBuffer.get(i)
            assertThat(
                "Float at index $i should be in [0.0, 1.0], was $value",
                value,
                allOf(greaterThanOrEqualTo(0.0f), lessThanOrEqualTo(1.0f))
            )
        }

        card.release()
    }

    @Test
    fun normalize_multipleRegions_returnsOneBufferPerRegion() {
        val card = loadGrayscaleCardFromAssets("test_score_card_no_header.jpg")
        val regions = listOf(
            RowRegion(top = 0, bottom = 50),
            RowRegion(top = 50, bottom = 100)
        )

        val results = OpenCVRowNormalizer().normalize(card, regions)

        assertThat(results, hasSize(2))
        results.forEach { rowImage ->
            assertThat(rowImage.buffer.capacity(), equalTo(66 * 640 * 4))
            assertThat(rowImage.buffer.order(), equalTo(ByteOrder.nativeOrder()))
            assertThat(rowImage.buffer.position(), equalTo(0))
        }

        card.release()
    }

    // ========== Helper Functions ==========

    private fun loadGrayscaleCardFromAssets(filename: String): Mat {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        return gray
    }
}
