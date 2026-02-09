package net.yakavenka.mlinference

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

/**
 * Instrumented test for TFLite model loading.
 * Part of Phase 2, Slice 2.3: TFLite Model Loading Stub.
 *
 * Verifies that:
 * - TFLite interpreter can load the model in Android test environment
 * - ScoreClassifier can process a dummy Mat without crashing
 * - Returns valid score in range 0-5
 */
@RunWith(AndroidJUnit4::class)
class TFLiteModelLoadingTest {
    private lateinit var classifier: TFLiteScoreClassifier
    private lateinit var context: Context

    @Before
    fun setUp() {
        // Initialize OpenCV
        val opencvInitialized = OpenCVLoader.initDebug()
        assertThat("OpenCV should initialize successfully", opencvInitialized, equalTo(true))

        // Get application context
        context = ApplicationProvider.getApplicationContext()

        // Load TFLite model
        classifier = TFLiteScoreClassifier(context, "score_classifier_model.tflite")
    }

    @After
    fun tearDown() {
        classifier.close()
    }

    @Test
    fun loadModel_succeeds() {
        // If we reach here without exceptions, model loaded successfully
        assertThat("Classifier should be initialized", classifier, notNullValue())
    }

    @Test
    fun classifyRow_withDummyMat_returnsValidScore() {
        // Create a dummy 640x66 grayscale Mat (filled with gray)
        val dummyMat = Mat(66, 640, CvType.CV_8UC1)
        dummyMat.setTo(Scalar(128.0))  // Mid-gray fill

        // Classify the dummy row
        val score = classifier.classifyRow(dummyMat)

        // Verify score is in valid range 0-5
        assertThat("Score should be in range 0-5", score, allOf(
            greaterThanOrEqualTo(0),
            lessThanOrEqualTo(5)
        ))

        // Cleanup
        dummyMat.release()
    }

    @Test
    fun classifyRow_withBlackMat_returnsValidScore() {
        // Create a black 640x66 grayscale Mat
        val blackMat = Mat(66, 640, CvType.CV_8UC1)
        blackMat.setTo(Scalar(0.0))  // Black fill

        val score = classifier.classifyRow(blackMat)

        assertThat("Score should be in range 0-5", score, allOf(
            greaterThanOrEqualTo(0),
            lessThanOrEqualTo(5)
        ))

        blackMat.release()
    }

    @Test
    fun classifyRow_withWhiteMat_returnsValidScore() {
        // Create a white 640x66 grayscale Mat
        val whiteMat = Mat(66, 640, CvType.CV_8UC1)
        whiteMat.setTo(Scalar(255.0))  // White fill

        val score = classifier.classifyRow(whiteMat)

        assertThat("Score should be in range 0-5", score, allOf(
            greaterThanOrEqualTo(0),
            lessThanOrEqualTo(5)
        ))

        whiteMat.release()
    }

    @Test
    fun stubClassifier_alwaysReturnsZero() {
        // Test the stub implementation separately
        val stubClassifier = StubScoreClassifier()
        val dummyMat = Mat(66, 640, CvType.CV_8UC1)
        dummyMat.setTo(Scalar(128.0))

        val score = stubClassifier.classifyRow(dummyMat)

        assertThat("Stub classifier should always return 0", score, equalTo(0))

        dummyMat.release()
    }
}
