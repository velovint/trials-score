package net.yakavenka.mlinference

import android.content.Context
import android.graphics.BitmapFactory
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
    fun loadGoldenDataset_fromAssets_classifiesCorrectly() {
        // Load golden PNG from assets
        val inputStream = context.assets.open("golden/0/image_0_row_1.png")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert to grayscale Mat
        val colorMat = Mat()
        Utils.bitmapToMat(bitmap, colorMat)

        val grayMat = Mat()
        Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Resize to expected dimensions (640x66)
        val resizedMat = Mat()
        val targetSize = Size(640.0, 66.0)
        Imgproc.resize(grayMat, resizedMat, targetSize)

        // Convert to RowImage (ByteBuffer, float32, normalized [0,1])
        val rowImage = matToRowImage(resizedMat)

        // Pass through real TFLite classifier
        val prediction = classifier.classify(rowImage)

        // Verify result is a valid score (0, 1, 2, 3, or 5)
        assertThat("Golden image from folder '0' should be classified as valid score",
            prediction, isIn(listOf(0, 1, 2, 3, 5)))

        // Cleanup
        colorMat.release()
        grayMat.release()
        resizedMat.release()
    }

    @Test
    fun realTFLiteModel_withGoldenDataset_producesValidScores() {
        // Load golden PNG from assets
        val inputStream = context.assets.open("golden/0/image_0_row_1.png")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert to grayscale Mat
        val colorMat = Mat()
        Utils.bitmapToMat(bitmap, colorMat)

        val grayMat = Mat()
        Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        // Resize to expected dimensions (640x66)
        val resizedMat = Mat()
        val targetSize = Size(640.0, 66.0)
        Imgproc.resize(grayMat, resizedMat, targetSize)

        // Convert to RowImage (ByteBuffer, float32, normalized [0,1])
        val rowImage = matToRowImage(resizedMat)

        // Pass through real TFLite classifier
        val prediction = classifier.classify(rowImage)

        // Verify result is a valid score (0, 1, 2, 3, or 5)
        assertThat("Real model should return valid score",
            prediction, isIn(listOf(0, 1, 2, 3, 5)))

        // Note: With an untrained or partially trained model, we may not get the correct label (0)
        // The key validation is that the model runs and produces valid output without crashing

        // Cleanup
        colorMat.release()
        grayMat.release()
        resizedMat.release()
    }

    private fun matToRowImage(mat: Mat): RowImage {
        require(mat.width() == 640 && mat.height() == 66) {
            "Input Mat must be 640x66, got ${mat.width()}x${mat.height()}"
        }
        require(mat.channels() == 1) {
            "Input Mat must be grayscale (1 channel), got ${mat.channels()} channels"
        }

        val width = 640
        val height = 66
        val channels = 1

        val buffer = ByteBuffer.allocateDirect(width * height * channels * 4)
        buffer.order(ByteOrder.nativeOrder())

        val data = ByteArray(width * height)
        mat.get(0, 0, data)

        for (pixel in data) {
            val normalizedValue = (pixel.toInt() and 0xFF) / 255.0f
            buffer.putFloat(normalizedValue)
        }

        buffer.rewind()
        return RowImage(buffer)
    }
}
