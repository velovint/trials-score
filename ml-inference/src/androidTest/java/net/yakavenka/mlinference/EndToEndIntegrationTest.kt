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
        CardImagePreprocessor.DEBUG_MODE = true
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

        // STEP 4: Classify rows and validate expected scores
        val expectedScores = listOf(1, 0, 0, 1, 1, 2, 0, 1, 0, 2)
        val classifier = TFLiteScoreClassifier(context, "score_classifier_model.tflite")
        val classificationResults = mutableListOf<Int>()

        for (rowIndex in 0 until minOf(expectedScores.size, rows.size)) {
            val row = rows[rowIndex]
            val resizedRow = Mat()
            val targetSize = Size(640.0, 66.0)
            Imgproc.resize(row, resizedRow, targetSize)

            val classificationResult = classifier.classifyRow(resizedRow)
            classificationResults.add(classificationResult)

            // Verify each result is a valid score (0, 1, 2, 3, or 5)
            assertThat("Row $rowIndex should return valid score",
                classificationResult, isIn(listOf(0, 1, 2, 3, 5)))

            resizedRow.release()
        }

        // STEP 5: Assert classification results match expected scores
        assertThat("Classification results should match expected scores",
            classificationResults, equalTo(expectedScores))

        // Cleanup
        classifier.close()
        grayMat.release()
        preprocessed.release()
        rows.forEach { it.release() }
    }
}
