package net.yakavenka.cardscanner

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

/**
 * Test data class representing a score card test case.
 *
 * @property filename Asset filename (e.g., "test_score_card_no_header.jpg")
 * @property description Human-readable description of the test card
 * @property expectedRows Expected number of rows to extract (default: 15)
 * @property minWhitePixelRatio Minimum ratio of white pixels after card detection (default: 0.70)
 */
data class TestCard(
    val filename: String,
    val description: String,
    val expectedRows: Int = 15,
    val minWhitePixelRatio: Double = 0.70
)

@RunWith(AndroidJUnit4::class)
class CardImagePreprocessorTest {

    private lateinit var testStorage: TestStorage

    /**
     * Test cards covering different scenarios.
     * Add new test cards here to automatically run all tests against them.
     */
    private val testCards = listOf(
        TestCard(
            filename = "test_score_card_no_header.jpg",
            description = "Standard score card"
        ),
        TestCard(
            filename = "score-card-uncropped.jpg",
            description = "Score card with background (tests card detection)"
        ),
        TestCard(
            filename = "score-card-sideways.jpg",
            description = "Sideways card with header gap (tests robust grid detection)"
        )
        // Add more test cards here as needed
    )

    @Before
    fun setup() {
        OpenCVLoader.initLocal()
        testStorage = TestStorage()
    }

    /**
     * Core preprocessing validation:
     * - Resizes to 640px width
     * - Maintains aspect ratio
     * - Converts to grayscale
     * - Extracts expected number of rows
     */
    @Test
    fun preprocessAndExtractRows_producesCorrectOutput() {
        testCards.forEach { testCard ->
            Log.i("CardImagePreprocessorTest", "Testing: ${testCard.description}")

            val image = loadTestImageFromAssets(testCard.filename)

            // Use OpenCVCardIsolator for card isolation
            val isolatedResult = OpenCVCardIsolator().isolate(image)
            val preprocessed = isolatedResult.getOrThrow()

            // Validate preprocessing
            assertThat("${testCard.description}: width", preprocessed.width(), equalTo(640))
            assertThat("${testCard.description}: height", preprocessed.height(), greaterThan(0))
            assertThat("${testCard.description}: grayscale", preprocessed.type(), equalTo(CvType.CV_8UC1))

            // Validate aspect ratio preservation
            val originalAspectRatio = image.height().toDouble() / image.width().toDouble()
            val preprocessedAspectRatio = preprocessed.height().toDouble() / preprocessed.width().toDouble()
            assertThat(
                "${testCard.description}: aspect ratio",
                preprocessedAspectRatio,
                closeTo(originalAspectRatio, 0.01)
            )

            // Validate row extraction
            val rows = CardImagePreprocessor.extractRowImages(preprocessed)
            assertThat("${testCard.description}: row count", rows.size, equalTo(testCard.expectedRows))

            rows.forEach { row ->
                assertThat("${testCard.description}: row not empty", row.empty(), equalTo(false))
                assertThat("${testCard.description}: row width", row.width(), equalTo(640))
                assertThat("${testCard.description}: row height", row.height(), greaterThan(0))
            }

            // Cleanup
            rows.forEach { it.release() }
            preprocessed.release()
            image.release()
        }
    }

    /**
     * Validates card detection by checking white pixel ratio.
     * Cards should be predominantly white after background removal.
     */
    @Test
    fun preprocessImage_detectsAndCropsCard() {
        testCards.forEach { testCard ->
            val image = loadTestImageFromAssets(testCard.filename)

            // Use OpenCVCardIsolator for card isolation
            val isolatedResult = OpenCVCardIsolator().isolate(image)
            val preprocessed = isolatedResult.getOrThrow()

            val whitePixelRatio = calculateWhitePixelPercentage(preprocessed, threshold = 200)

            Log.i(
                "CardImagePreprocessorTest",
                "${testCard.description}: white pixel ratio = ${(whitePixelRatio * 100).format(2)}%"
            )

            assertThat(
                "${testCard.description}: should be predominantly white after card detection",
                whitePixelRatio,
                greaterThan(testCard.minWhitePixelRatio)
            )

            preprocessed.release()
            image.release()
        }
    }

    /**
     * Validates row order using template matching.
     * First row should contain section "1", last row should contain section "15".
     * This catches reversed or upside-down row extraction.
     */
    @Test
    fun extractRowImages_correctOrder_matchesSectionNumbers() {
        // Load templates once
        val template1 = loadTestImageFromAssets("template_section_1.png")
        val template15 = loadTestImageFromAssets("template_section_15.png")

        testCards.forEach { testCard ->
            val image = loadTestImageFromAssets(testCard.filename)

            // Use OpenCVCardIsolator for card isolation
            val preprocessed = OpenCVCardIsolator().isolate(image).getOrThrow()
            val rows = CardImagePreprocessor.extractRowImages(preprocessed)

            // Match first and last rows
            val firstRowMatch = matchTemplate(rows[0], template1)
            val lastRowMatch = matchTemplate(rows[14], template15)

            Log.i(
                "CardImagePreprocessorTest",
                "${testCard.description}: section 1 match=${firstRowMatch.format(3)}, section 15 match=${lastRowMatch.format(3)}"
            )

            // Save debug images to TestStorage
            saveMatToTestStorage(rows[0], "${testCard.filename}_row_00.png")
            saveMatToTestStorage(rows[14], "${testCard.filename}_row_14.png")

            assertThat(
                "${testCard.description}: first row should match section 1 (score: ${firstRowMatch.format(3)})",
                firstRowMatch,
                greaterThan(0.7)
            )

            assertThat(
                "${testCard.description}: last row should match section 15 (score: ${lastRowMatch.format(3)})",
                lastRowMatch,
                greaterThan(0.7)
            )

            // Cleanup
            rows.forEach { it.release() }
            preprocessed.release()
            image.release()
        }

        // Save templates for reference
        saveMatToTestStorage(template1, "template_section_1.png")
        saveMatToTestStorage(template15, "template_section_15.png")

        template1.release()
        template15.release()

        Log.i(
            "CardImagePreprocessorTest",
            "Debug images saved to: build/outputs/managed_device_android_test_additional_output/"
        )
    }

    /**
     * Debug test for troubleshooting preprocessing issues.
     * Enables DEBUG_MODE to save intermediate images (edge detection, contours, etc.).
     * Remove @Ignore annotation to run manually.
     */
    @Ignore("Debug test - enable manually when troubleshooting")
    @Test
    fun debugPreprocessing_savesIntermediateImages() {
        CardImagePreprocessor.DEBUG_MODE = true
        CardImagePreprocessor.DEBUG_OUTPUT_DIR = "/sdcard/Download/card-debug"

        val debugDir = File("/sdcard/Download/card-debug")
        debugDir.mkdirs()

        testCards.forEach { testCard ->
            Log.i("CardImagePreprocessorTest", "=== DEBUG: ${testCard.description} ===")

            val image = loadTestImageFromAssets(testCard.filename)
            Log.i("CardImagePreprocessorTest", "Input: ${image.width()}×${image.height()}")

            // Use OpenCVCardIsolator for card isolation
            val isolatedResult = OpenCVCardIsolator().isolate(image)
            val preprocessed = isolatedResult.getOrThrow()
            Log.i("CardImagePreprocessorTest", "Output: ${preprocessed.width()}×${preprocessed.height()}")

            val whitePixelRatio = calculateWhitePixelPercentage(preprocessed, threshold = 200)
            Log.i("CardImagePreprocessorTest", "White pixels: ${(whitePixelRatio * 100).format(2)}%")

            val outputPath = "/sdcard/Download/card-debug/${testCard.filename}_preprocessed.png"
            Imgcodecs.imwrite(outputPath, preprocessed)
            Log.i("CardImagePreprocessorTest", "Saved: $outputPath")

            preprocessed.release()
            image.release()
        }

        Log.i("CardImagePreprocessorTest", "To extract debug images:")
        Log.i("CardImagePreprocessorTest", "  adb pull /sdcard/Download/card-debug ./debug-output")

        CardImagePreprocessor.DEBUG_MODE = false
    }

    // ========== Helper Functions ==========

    private fun loadTestImageFromAssets(filename: String): Mat {
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

    private fun saveMatToTestStorage(mat: Mat, filename: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tempFile = File(context.cacheDir, filename)
        val writeSuccess = Imgcodecs.imwrite(tempFile.absolutePath, mat)

        if (writeSuccess) {
            testStorage.openOutputFile(filename).use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile.delete()
        }
    }

    private fun calculateWhitePixelPercentage(image: Mat, threshold: Int): Double {
        require(image.type() == CvType.CV_8UC1) { "Image must be grayscale" }
        require(threshold in 0..255) { "Threshold must be 0-255" }

        val binaryImage = Mat()
        Imgproc.threshold(image, binaryImage, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)

        val whitePixelCount = org.opencv.core.Core.countNonZero(binaryImage)
        val totalPixels = image.total().toInt()

        binaryImage.release()

        return whitePixelCount.toDouble() / totalPixels
    }

    private fun matchTemplate(rowImage: Mat, template: Mat): Double {
        require(rowImage.type() == CvType.CV_8UC1) { "Row image must be grayscale" }
        require(template.type() == CvType.CV_8UC1) { "Template must be grayscale" }

        val result = Mat()
        Imgproc.matchTemplate(rowImage, template, result, Imgproc.TM_CCOEFF_NORMED)

        val minMaxLoc = org.opencv.core.Core.minMaxLoc(result)
        val matchScore = minMaxLoc.maxVal

        result.release()

        return matchScore
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}
