package net.yakavenka.mlinference

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.yakavenka.cardscanner.CardImagePreprocessor
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.isIn
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * End-to-end integration test verifying the complete ML pipeline:
 * raw image → CV processing (shared-cv) → real TFLite model inference → result
 *
 * This test proves that ml-inference module can:
 * - Depend on shared-cv for image preprocessing
 * - Process real score card images
 * - Classify extracted rows using the actual TFLite model
 * - Produce valid score predictions (0, 1, 2, 3, or 5)
 *
 * Architecture: ml-inference depends on shared-cv (correct dependency direction)
 */
@RunWith(AndroidJUnit4::class)
class EndToEndIntegrationTest {

    @Before
    fun setUp() {
        // Initialize OpenCV native library
        OpenCVLoader.initLocal()

        // Disable debug mode to avoid file I/O during tests
        CardImagePreprocessor.DEBUG_MODE = false
    }

    @Test
    fun endToEndPipeline_withRealTFLiteModel_producesValidScores() {
        // STEP 1: Load real score card image from assets
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // STEP 2: Convert to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale (CardImagePreprocessor expects grayscale)
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        // STEP 3: Use CardImagePreprocessor from :shared-cv module
        val preprocessed = CardImagePreprocessor.preprocessImage(grayMat)
        val rows = CardImagePreprocessor.extractRowImages(preprocessed)

        // Verify we got 15 rows from preprocessing
        assertThat("Should extract 15 rows from score card", rows.size, equalTo(15))
        assertTrue("Should have at least one row to classify", rows.isNotEmpty())

        // STEP 4: Resize first row to model's expected dimensions (640x66)
        val firstRow = rows[0]
        val resizedRow = Mat()
        val targetSize = Size(640.0, 66.0)
        Imgproc.resize(firstRow, resizedRow, targetSize)

        // STEP 5: Classify resized row using real TFLiteScoreClassifier
        val classifier = TFLiteScoreClassifier(context, "score_classifier_model.tflite")
        val classificationResult = classifier.classifyRow(resizedRow)

        // STEP 6: Assert final result is a valid score (0, 1, 2, 3, or 5)
        assertThat("Real TFLite classifier should return valid score",
            classificationResult, isIn(listOf(0, 1, 2, 3, 5)))

        // Verify resized row dimensions match model expectations
        assertThat("Resized row width should be 640px", resizedRow.width(), equalTo(640))
        assertThat("Resized row height should be 66px", resizedRow.height(), equalTo(66))

        // Cleanup
        classifier.close()
        grayMat.release()
        preprocessed.release()
        rows.forEach { it.release() }
        resizedRow.release()
    }
}
