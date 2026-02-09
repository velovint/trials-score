package net.yakavenka.mlinference

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.yakavenka.cardscanner.CardImagePreprocessor
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * End-to-end stub test verifying the complete data flow:
 * raw image → CV processing (shared-cv) → ML inference → result
 *
 * This test proves that ml-inference module can:
 * - Depend on shared-cv for image preprocessing
 * - Process real score card images
 * - Classify extracted rows
 *
 * Architecture: ml-inference depends on shared-cv (correct dependency direction)
 */
@RunWith(AndroidJUnit4::class)
class EndToEndStubTest {

    @Before
    fun setUp() {
        // Initialize OpenCV native library
        OpenCVLoader.initLocal()

        // Disable debug mode to avoid file I/O during tests
        CardImagePreprocessor.DEBUG_MODE = false
    }

    @Test
    fun endToEndPipeline_processesImageAndClassifiesRow() {
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

        // STEP 4: Classify first row using StubScoreClassifier
        val classifier = StubScoreClassifier()
        val firstRow = rows[0]
        val classificationResult = classifier.classifyRow(firstRow)

        // STEP 5: Assert final result is 0 (stub always returns 0)
        assertThat("Stub classifier should return 0", classificationResult, equalTo(0))

        // Verify row dimensions are correct for classification (640x66 expected)
        assertThat("Row width should be 640px", firstRow.width(), equalTo(640))
        assertTrue("Row height should be positive", firstRow.height() > 0)

        // Cleanup
        grayMat.release()
        preprocessed.release()
        rows.forEach { it.release() }
    }

    @Test
    fun endToEndPipeline_classifiesAllRowsFromRealImage() {
        // Load and process real image
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        // Extract rows using CardImagePreprocessor
        val preprocessed = CardImagePreprocessor.preprocessImage(grayMat)
        val rows = CardImagePreprocessor.extractRowImages(preprocessed)

        // Classify all rows
        val classifier = StubScoreClassifier()
        val results = rows.map { row ->
            classifier.classifyRow(row)
        }

        // Verify all results are 0 (stub behavior)
        assertThat("Should classify all 15 rows", results.size, equalTo(15))
        results.forEach { result ->
            assertThat("Each classification should return 0", result, equalTo(0))
        }

        // Verify all rows have proper dimensions
        rows.forEach { row ->
            assertThat("Row width should be 640px", row.width(), equalTo(640))
            assertTrue("Row height should be positive", row.height() > 0)
        }

        // Cleanup
        grayMat.release()
        preprocessed.release()
        rows.forEach { it.release() }
    }

    @Test
    fun endToEndPipeline_verifyModuleDependency() {
        // This test verifies the architectural dependency is correct:
        // ml-inference -> shared-cv (can import and use CardImagePreprocessor)

        // Create a simple test Mat
        val testMat = Mat(960, 640, org.opencv.core.CvType.CV_8UC1)
        testMat.setTo(org.opencv.core.Scalar(128.0))

        // Verify we can use CardImagePreprocessor from shared-cv
        val preprocessed = CardImagePreprocessor.preprocessImage(testMat)
        assertTrue("Should be able to preprocess image", preprocessed.width() > 0)

        // Verify we can use StubScoreClassifier from ml-inference
        val classifier = StubScoreClassifier()
        val result = classifier.classifyRow(preprocessed)
        assertThat("Should be able to classify", result, equalTo(0))

        // Cleanup
        testMat.release()
        preprocessed.release()
    }
}
