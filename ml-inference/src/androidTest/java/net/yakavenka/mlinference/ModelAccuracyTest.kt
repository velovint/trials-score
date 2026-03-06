package net.yakavenka.mlinference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.yakavenka.cardscanner.RowImage
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
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Tests the real TFLite model validation using golden dataset.
 * Verifies that the TFLiteRowClassifier loads correctly and produces valid classifications.
 */
@RunWith(AndroidJUnit4::class)
class ModelAccuracyTest {

    private lateinit var context: Context
    private lateinit var classifier: TFLiteRowClassifier

    @Before
    fun setUp() {
        OpenCVLoader.initDebug()
        context = ApplicationProvider.getApplicationContext()
        classifier = TFLiteRowClassifier(context)
    }

    @After
    fun tearDown() {
        classifier.close()
    }

    @Test
    fun classify_goldenDataset_accuracy95Percent() {
        val labelClasses = listOf(0, 1, 2, 3, 5)
        var correct = 0
        var total = 0

        for (label in labelClasses) {
            val folder = "golden/$label"
            val files: List<String> = try { context.assets.list(folder)?.toList() ?: emptyList() }
                        catch (e: Exception) { emptyList() }

            for (filename in files) {
                val bitmap = context.assets.open("$folder/$filename")
                    .use { BitmapFactory.decodeStream(it) }
                val prediction = classifier.classify(bitmapToRowImage(bitmap))
                if (prediction == label) correct++
                total++
            }
            Log.i(TAG, "label $label: ${files.size} images")
        }

        assertThat("Need at least 50 golden images", total, greaterThan(50))
        val accuracy = correct.toDouble() / total
        Log.i(TAG, "Accuracy: $correct/$total = ${"%.1f".format(accuracy * 100)}%")
        assertThat("Model accuracy must be >= 95%", accuracy, greaterThanOrEqualTo(0.95))
    }

    private fun bitmapToRowImage(bitmap: Bitmap): RowImage {
        val bgra = Mat()
        Utils.bitmapToMat(bitmap, bgra)          // bitmapToMat produces BGRA

        val gray = Mat()
        Imgproc.cvtColor(bgra, gray, Imgproc.COLOR_BGRA2GRAY)
        bgra.release()

        val resized = Mat()
        Imgproc.resize(gray, resized, Size(640.0, 66.0))
        gray.release()

        val float32 = Mat()
        resized.convertTo(float32, CvType.CV_32F, 1.0 / 255.0)
        resized.release()

        val floats = FloatArray(66 * 640)
        float32.get(0, 0, floats)
        float32.release()

        val buffer = ByteBuffer.allocateDirect(66 * 640 * 4).order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(floats)
        buffer.rewind()
        return RowImage(buffer)
    }

    companion object {
        private const val TAG = "ModelAccuracyTest"
    }
}
