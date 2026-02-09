package net.yakavenka.mlinference

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar

/**
 * Tests the model accuracy validation logic using stub classifier.
 * Verifies that accuracy calculation works correctly with known data.
 */
@RunWith(AndroidJUnit4::class)
class ModelAccuracyTest {

    private lateinit var classifier: ScoreClassifier

    @Before
    fun setUp() {
        OpenCVLoader.initDebug()
        classifier = StubScoreClassifier()
    }

    @Test
    fun calculateAccuracy_withPerfectPredictions_returns100Percent() {
        // Create 10 fake "golden" Mat samples (640x66 grayscale)
        val goldenSamples = (0 until 10).map {
            Mat(66, 640, CvType.CV_8UC1).apply {
                setTo(Scalar(128.0))  // Gray fill
            }
        }

        // Expected labels - all 0 (matches stub classifier output)
        val expectedLabels = List(10) { 0 }

        // Run classifier on each sample
        val predictions = goldenSamples.map { sample ->
            classifier.classifyRow(sample)
        }

        // Calculate accuracy
        val correctPredictions = predictions.zip(expectedLabels).count { (pred, label) ->
            pred == label
        }
        val accuracy = (correctPredictions.toDouble() / predictions.size) * 100.0

        // Assert accuracy is 100%
        assertThat(accuracy, equalTo(100.0))

        // Cleanup
        goldenSamples.forEach { it.release() }
    }

    @Test
    fun calculateAccuracy_withMismatchedLabels_returnsLowAccuracy() {
        // Create 10 fake "golden" Mat samples
        val goldenSamples = (0 until 10).map {
            Mat(66, 640, CvType.CV_8UC1).apply {
                setTo(Scalar(128.0))
            }
        }

        // Expected labels - intentionally different from stub output (0)
        // Using mix of scores 1-5 to simulate mismatch
        val expectedLabels = listOf(1, 2, 3, 5, 1, 2, 3, 5, 1, 2)

        // Run classifier on each sample (stub always returns 0)
        val predictions = goldenSamples.map { sample ->
            classifier.classifyRow(sample)
        }

        // Calculate accuracy
        val correctPredictions = predictions.zip(expectedLabels).count { (pred, label) ->
            pred == label
        }
        val accuracy = (correctPredictions.toDouble() / predictions.size) * 100.0

        // Assert accuracy is 0% (no matches since stub returns 0, labels are 1-5)
        assertThat(accuracy, equalTo(0.0))
        assertThat("Accuracy should be less than 95% threshold", accuracy, lessThan(95.0))

        // Cleanup
        goldenSamples.forEach { it.release() }
    }

    @Test
    fun calculateAccuracy_withPartialMatch_returnsCorrectPercentage() {
        // Create 10 fake samples
        val goldenSamples = (0 until 10).map {
            Mat(66, 640, CvType.CV_8UC1).apply {
                setTo(Scalar(128.0))
            }
        }

        // Expected labels - 7 zeros (match), 3 non-zeros (no match)
        val expectedLabels = listOf(0, 0, 0, 0, 0, 0, 0, 1, 2, 3)

        // Run classifier (always returns 0)
        val predictions = goldenSamples.map { sample ->
            classifier.classifyRow(sample)
        }

        // Calculate accuracy
        val correctPredictions = predictions.zip(expectedLabels).count { (pred, label) ->
            pred == label
        }
        val accuracy = (correctPredictions.toDouble() / predictions.size) * 100.0

        // Assert accuracy is 70% (7 out of 10 match)
        assertThat(accuracy, equalTo(70.0))

        // Cleanup
        goldenSamples.forEach { it.release() }
    }

    @Test
    fun loadGoldenDataset_fromAssets_classifiesCorrectly() {
        // Load golden PNG from assets
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inputStream = context.assets.open("golden/0/image_0_row_1.png")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Pass through classifier
        val prediction = classifier.classifyRow(mat)

        // Verify result matches the folder name (0)
        assertThat("Golden image from folder '0' should be classified as 0",
            prediction, equalTo(0))

        // Cleanup
        mat.release()
    }
}
