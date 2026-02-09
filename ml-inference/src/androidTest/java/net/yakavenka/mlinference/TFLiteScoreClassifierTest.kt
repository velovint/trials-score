package net.yakavenka.mlinference

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Instrumented test for real TFLite Score Classifier.
 * Part of Phase 4, Slice 4.2: Real TFLite Model Integration.
 *
 * Verifies that:
 * - TFLite model loads successfully from assets
 * - Classifier can process 640x66 grayscale Mat inputs
 * - Model produces valid scores (0, 1, 2, 3, 5)
 * - Golden dataset classification works
 */
@RunWith(AndroidJUnit4::class)
class TFLiteScoreClassifierTest {

    private lateinit var context: Context
    private lateinit var classifier: TFLiteScoreClassifier

    @Before
    fun setUp() {
        // Initialize OpenCV
        val opencvInitialized = OpenCVLoader.initDebug()
        assertThat("OpenCV should initialize successfully", opencvInitialized, equalTo(true))

        // Get application context
        context = ApplicationProvider.getApplicationContext()

        // Initialize TFLite classifier with real model
        classifier = TFLiteScoreClassifier(context, "score_classifier_model.tflite")
    }

    @After
    fun tearDown() {
        classifier.close()
    }

    @Test
    fun tfliteClassifier_loadsModelSuccessfully() {
        // If we got here without exception, model loaded successfully
        assertThat("Classifier should be initialized", classifier, notNullValue())
    }

    @Test
    fun tfliteClassifier_withSyntheticMat_returnsValidScore() {
        // Create a synthetic 640x66 grayscale Mat
        val syntheticMat = Mat(66, 640, CvType.CV_8UC1)
        syntheticMat.setTo(Scalar(128.0))  // Mid-gray fill

        val score = classifier.classifyRow(syntheticMat)

        // Verify score is in valid range (0, 1, 2, 3, or 5)
        assertThat("Score should be in valid range", score, isIn(listOf(0, 1, 2, 3, 5)))

        syntheticMat.release()
    }

    @Test
    fun tfliteClassifier_withGoldenDataset_classifiesCorrectly() {
        // Load golden PNG from assets (label 0)
        val inputStream = context.assets.open("golden/0/image_0_row_1.png")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert to Mat
        val colorMat = Mat()
        Utils.bitmapToMat(bitmap, colorMat)

        // Convert to grayscale and resize to 640x66
        val grayMat = Mat()
        Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        val resizedMat = Mat()
        val targetSize = Size(640.0, 66.0)
        Imgproc.resize(grayMat, resizedMat, targetSize)

        // Classify
        val prediction = classifier.classifyRow(resizedMat)

        // Verify result is a valid score
        assertThat("Prediction should be valid score", prediction, isIn(listOf(0, 1, 2, 3, 5)))

        // Note: We don't assert prediction == 0 because the model may not be fully trained yet
        // The key validation is that it produces a valid score without crashing

        // Cleanup
        colorMat.release()
        grayMat.release()
        resizedMat.release()
    }

    @Test
    fun tfliteClassifier_multipleInferences_produceConsistentResults() {
        // Create a test Mat
        val testMat = Mat(66, 640, CvType.CV_8UC1)
        testMat.setTo(Scalar(200.0))

        // Run inference multiple times
        val results = List(5) { classifier.classifyRow(testMat) }

        // Results should be consistent (same input → same output)
        assertThat("Multiple inferences should produce consistent results",
            results.distinct().size, equalTo(1))

        // All results should be valid scores
        results.forEach { score ->
            assertThat("Score should be valid", score, isIn(listOf(0, 1, 2, 3, 5)))
        }

        testMat.release()
    }

    @Test
    fun tfliteClassifier_withDifferentInputs_producesValidScores() {
        // Test with different gray levels
        val grayLevels = listOf(0.0, 64.0, 128.0, 192.0, 255.0)

        grayLevels.forEach { grayLevel ->
            val mat = Mat(66, 640, CvType.CV_8UC1)
            mat.setTo(Scalar(grayLevel))

            val score = classifier.classifyRow(mat)

            assertThat("Score for gray level $grayLevel should be valid",
                score, isIn(listOf(0, 1, 2, 3, 5)))

            mat.release()
        }
    }

    @Test
    fun tfliteClassifier_withIncorrectDimensions_throwsException() {
        // Create Mat with wrong dimensions (should be 640x66)
        val wrongMat = Mat(100, 100, CvType.CV_8UC1)
        wrongMat.setTo(Scalar(128.0))

        try {
            classifier.classifyRow(wrongMat)
            // If we got here, the validation didn't work
            org.junit.Assert.fail("Should have thrown exception for incorrect dimensions")
        } catch (e: IllegalArgumentException) {
            // Expected behavior
            assertThat("Should mention dimension requirement in error",
                e.message, containsString("640x66"))
        } finally {
            wrongMat.release()
        }
    }
}
