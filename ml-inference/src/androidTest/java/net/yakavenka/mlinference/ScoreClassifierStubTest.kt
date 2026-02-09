package net.yakavenka.mlinference

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

/**
 * Instrumented test for Score Classifier stub implementation.
 * Part of Phase 2, Slice 2.3: TFLite Model Loading Stub.
 *
 * Verifies that:
 * - ScoreClassifier interface works correctly
 * - Stub implementation can process Mat without crashing
 * - Returns valid score in expected range
 *
 * NOTE: Real TFLite model loading test is deferred due to model version incompatibility.
 * The existing model uses FULLY_CONNECTED v12 which requires TFLite 2.17+ (not yet released).
 * This will be addressed in Phase 4, Slice 4.2: Real TFLite Model Integration.
 */
@RunWith(AndroidJUnit4::class)
class ScoreClassifierStubTest {

    @Before
    fun setUp() {
        // Initialize OpenCV
        val opencvInitialized = OpenCVLoader.initDebug()
        assertThat("OpenCV should initialize successfully", opencvInitialized, equalTo(true))
    }

    @Test
    fun stubClassifier_withDummyMat_returnsZero() {
        val classifier = StubScoreClassifier()
        val dummyMat = Mat(66, 640, CvType.CV_8UC1)
        dummyMat.setTo(Scalar(128.0))  // Mid-gray fill

        val score = classifier.classifyRow(dummyMat)

        assertThat("Stub classifier should return 0", score, equalTo(0))

        dummyMat.release()
    }

    @Test
    fun stubClassifier_withBlackMat_returnsZero() {
        val classifier = StubScoreClassifier()
        val blackMat = Mat(66, 640, CvType.CV_8UC1)
        blackMat.setTo(Scalar(0.0))  // Black fill

        val score = classifier.classifyRow(blackMat)

        assertThat("Stub classifier should return 0", score, equalTo(0))

        blackMat.release()
    }

    @Test
    fun stubClassifier_withWhiteMat_returnsZero() {
        val classifier = StubScoreClassifier()
        val whiteMat = Mat(66, 640, CvType.CV_8UC1)
        whiteMat.setTo(Scalar(255.0))  // White fill

        val score = classifier.classifyRow(whiteMat)

        assertThat("Stub classifier should return 0", score, equalTo(0))

        whiteMat.release()
    }

    @Test
    fun stubClassifier_multipleCalls_consistentResults() {
        val classifier = StubScoreClassifier()
        val dummyMat = Mat(66, 640, CvType.CV_8UC1)
        dummyMat.setTo(Scalar(200.0))

        // Call multiple times
        val results = List(10) { classifier.classifyRow(dummyMat) }

        // All results should be 0
        assertThat("All results should be 0", results, everyItem(equalTo(0)))

        dummyMat.release()
    }

    @Test
    fun scoreClassifierInterface_canAcceptDifferentImplementations() {
        // Verify interface can be implemented
        val classifiers: List<ScoreClassifier> = listOf(
            StubScoreClassifier()
        )

        val dummyMat = Mat(66, 640, CvType.CV_8UC1)
        dummyMat.setTo(Scalar(128.0))

        classifiers.forEach { classifier ->
            val score = classifier.classifyRow(dummyMat)
            assertThat("Score should be in valid range 0-5", score, allOf(
                greaterThanOrEqualTo(0),
                lessThanOrEqualTo(5)
            ))
        }

        dummyMat.release()
    }
}
