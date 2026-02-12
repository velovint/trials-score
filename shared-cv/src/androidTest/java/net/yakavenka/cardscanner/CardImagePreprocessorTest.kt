package net.yakavenka.cardscanner

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
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

@RunWith(AndroidJUnit4::class)
class CardImagePreprocessorTest {

    private lateinit var testImage: Mat

    @Before
    fun setup() {
        // Load OpenCV native library
        OpenCVLoader.initLocal()

        // Load test image for preprocessing tests
        testImage = loadTestImageFromAssets("test_score_card.jpg")
    }

    @After
    fun teardown() {
        testImage.release()
    }

    @Test
    fun preprocessImage_withValidImage_resizesTo640Width() {
        val preprocessed = CardImagePreprocessor.preprocessImage(testImage)

        assertThat(preprocessed.width(), equalTo(640))
        assertThat(preprocessed.height(), greaterThan(0))

        // Verify aspect ratio is preserved
        val originalAspectRatio = testImage.height().toDouble() / testImage.width().toDouble()
        val preprocessedAspectRatio = preprocessed.height().toDouble() / preprocessed.width().toDouble()
        assertThat(preprocessedAspectRatio, closeTo(originalAspectRatio, 0.01))

        // Verify grayscale format
        assertThat(preprocessed.type(), equalTo(CvType.CV_8UC1))

        preprocessed.release()
    }

    @Test
    fun preprocessImage_withSmallImage_enlargesToTargetWidth() {
        // Create small 100x100 test image
        val smallImage = Mat(100, 100, CvType.CV_8UC1)

        val preprocessed = CardImagePreprocessor.preprocessImage(smallImage)

        // Should enlarge to 640px width
        assertThat(preprocessed.width(), equalTo(640))
        assertThat(preprocessed.height(), equalTo(640))  // Square aspect ratio preserved

        smallImage.release()
        preprocessed.release()
    }

    @Test
    fun preprocessImage_withCardAndBackground_cropsToWhiteCardOnly() {
        val preprocessed = CardImagePreprocessor.preprocessImage(loadTestImageFromAssets("score-card-uncropped.jpg"))

        // Calculate percentage of white/near-white pixels (>= 200 on 0-255 scale)
        val whitePixelPercentage = calculateWhitePixelPercentage(preprocessed, threshold = 200)

        // Save preprocessed image to device external storage (can be pulled with adb pull)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDir = File(context.getExternalFilesDir(null), "test-outputs")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "preprocessed_card.png")
        val success = Imgcodecs.imwrite(outputFile.absolutePath, preprocessed)

        // Also save to /sdcard for easy access (user-accessible location)
        val sdcardPath = "/sdcard/Download/preprocessed_card.png"
        Imgcodecs.imwrite(sdcardPath, preprocessed)

        val message = """
            Preprocessed image saved to:
              - ${outputFile.absolutePath}
              - $sdcardPath
            Image dimensions: ${preprocessed.width()}x${preprocessed.height()}
            White pixel percentage: ${(whitePixelPercentage * 100).format(2)}%
            Save success: $success

            Pull from device with: adb pull $sdcardPath ./preprocessed_card.png
        """.trimIndent()

        Log.i("CardImagePreprocessorTest", message)
        println(message)

        preprocessed.release()
        // Score card should be predominantly white (>70% white pixels)
        // This indicates successful card detection and cropping, removing background
        assertThat(whitePixelPercentage, greaterThan(0.70))
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)

    /**
     * Debug test for troubleshooting card detection issues.
     * Enable DEBUG_MODE to save intermediate images (edge detection, etc.) to device storage.
     * Remove @Ignore annotation to run this test.
     */
    @Ignore("Debug test - enable manually when troubleshooting card detection")
    @Test
    fun debugCardDetection_withRealCameraImage() {
        // Enable debug mode to save intermediate images
        CardImagePreprocessor.DEBUG_MODE = true
        CardImagePreprocessor.DEBUG_OUTPUT_DIR = "/sdcard/Download/card-debug"

        // Create debug output directory
        val debugDir = File("/sdcard/Download/card-debug")
        debugDir.mkdirs()

        // Load camera-like test image
        val cameraImage = loadTestImageFromAssets("score-card-uncropped.jpg")

        Log.i("CardImagePreprocessorTest", "=== DEBUG CARD DETECTION ===")
        Log.i("CardImagePreprocessorTest", "Input image: ${cameraImage.width()}×${cameraImage.height()}")

        // Run preprocessing with debug logging
        val preprocessed = CardImagePreprocessor.preprocessImage(cameraImage)

        Log.i("CardImagePreprocessorTest", "Output image: ${preprocessed.width()}×${preprocessed.height()}")
        Log.i("CardImagePreprocessorTest", "Size changed: ${preprocessed.width() != cameraImage.width() || preprocessed.height() != cameraImage.height()}")

        // Calculate white pixel percentage
        val whitePixelPercentage = calculateWhitePixelPercentage(preprocessed, threshold = 200)
        Log.i("CardImagePreprocessorTest", "White pixel percentage: ${(whitePixelPercentage * 100).format(2)}%")

        // Save final preprocessed image
        val outputPath = "/sdcard/Download/card-debug/final_preprocessed.png"
        Imgcodecs.imwrite(outputPath, preprocessed)
        Log.i("CardImagePreprocessorTest", "Final image saved to: $outputPath")

        Log.i("CardImagePreprocessorTest", "=== END DEBUG ===")
        Log.i("CardImagePreprocessorTest", "")
        Log.i("CardImagePreprocessorTest", "To extract debug images, run:")
        Log.i("CardImagePreprocessorTest", "  adb pull /sdcard/Download/card-debug ./debug-output")

        // Clean up
        cameraImage.release()
        preprocessed.release()

        // Disable debug mode after test
        CardImagePreprocessor.DEBUG_MODE = false
    }

    @Test
    fun extractRowImages_withPreprocessedImage_returns15Rows() {
        val preprocessed = CardImagePreprocessor.preprocessImage(testImage)

        val rowImages = CardImagePreprocessor.extractRowImages(preprocessed)

        assertThat(rowImages.size, equalTo(15))

        // Each row should be non-empty
        rowImages.forEach { row ->
            assertThat(row.empty(), equalTo(false))
            assertThat(row.width(), greaterThan(0))
            assertThat(row.height(), greaterThan(0))
        }

        // Clean up
        rowImages.forEach { it.release() }
        preprocessed.release()
    }

    @Test
    fun extractRowImages_releasesAllMatsWithoutError() {
        val preprocessed = CardImagePreprocessor.preprocessImage(testImage)

        val rowImages = CardImagePreprocessor.extractRowImages(preprocessed)

        // Verify we can release all Mats without errors
        rowImages.forEach { row ->
            row.release()
            assertThat(row.empty(), equalTo(true))
        }

        preprocessed.release()
        assertThat(preprocessed.empty(), equalTo(true))
    }

    @Test
    fun extractRowImages_correctOrder_firstAndLastSectionsDetected() {
        // Preprocess and extract rows
        val preprocessed = CardImagePreprocessor.preprocessImage(testImage)
        val rowImages = CardImagePreprocessor.extractRowImages(preprocessed)

        assertThat("Should extract 15 rows", rowImages.size, equalTo(15))

        // Load section number templates
        val template1 = loadTestImageFromAssets("template_section_1.png")
        val template15 = loadTestImageFromAssets("template_section_15.png")

        // Save debug images using TestStorage
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testStorage = TestStorage()

        // Helper function to save Mat to TestStorage
        fun saveMatToTestStorage(mat: Mat, filename: String) {
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

        // Save extracted rows for debugging
        saveMatToTestStorage(rowImages[0], "row_00_first.png")
        saveMatToTestStorage(rowImages[14], "row_14_last.png")
        saveMatToTestStorage(template1, "template_1.png")
        saveMatToTestStorage(template15, "template_15.png")

        // Verify first row contains section number "1"
        val firstRowMatch = matchTemplate(rowImages[0], template1)
        Log.i("CardImagePreprocessorTest", "First row (section 1) match score: $firstRowMatch")

        // Verify last row contains section number "15"
        val lastRowMatch = matchTemplate(rowImages[14], template15)
        Log.i("CardImagePreprocessorTest", "Last row (section 15) match score: $lastRowMatch")

        // Log match scores
        val debugMessage = """
            Row order validation completed:

            Match scores:
              - Section 1 (row 0): ${firstRowMatch.format(3)}
              - Section 15 (row 14): ${lastRowMatch.format(3)}

            Debug images saved to TestStorage:
              - row_00_first.png (extracted first row)
              - row_14_last.png (extracted last row)
              - template_1.png (section 1 template)
              - template_15.png (section 15 template)

            Find outputs in: build/outputs/managed_device_android_test_additional_output/
            or: build/outputs/androidTest-results/connected/
        """.trimIndent()

        Log.i("CardImagePreprocessorTest", debugMessage)
        println(debugMessage)

        assertThat(
            "First row should contain section number '1' (match score: ${firstRowMatch.format(3)})",
            firstRowMatch,
            greaterThan(0.7)
        )

        assertThat(
            "Last row should contain section number '15' (match score: ${lastRowMatch.format(3)})",
            lastRowMatch,
            greaterThan(0.7)
        )

        // Clean up
        template1.release()
        template15.release()
        rowImages.forEach { it.release() }
        preprocessed.release()
    }

    /**
     * Load test image from androidTest/assets and convert to grayscale Mat.
     */
    private fun loadTestImageFromAssets(filename: String): Mat {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)

        // Decode bitmap from input stream
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert bitmap to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        return gray
    }

    /**
     * Calculate the percentage of pixels with intensity >= threshold using OpenCV.
     * Used to verify card detection cropped to white card area.
     *
     * @param image Grayscale Mat (CV_8UC1)
     * @param threshold Pixel intensity threshold (0-255)
     * @return Percentage of pixels >= threshold (0.0 to 1.0)
     */
    private fun calculateWhitePixelPercentage(image: Mat, threshold: Int): Double {
        require(image.type() == CvType.CV_8UC1) { "Image must be grayscale (CV_8UC1)" }
        require(threshold in 0..255) { "Threshold must be in range 0-255" }

        // Create binary image: pixels >= threshold become 255, others become 0
        val binaryImage = Mat()
        Imgproc.threshold(image, binaryImage, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)

        // Count non-zero (white) pixels efficiently
        val whitePixelCount = org.opencv.core.Core.countNonZero(binaryImage)
        val totalPixels = image.total().toInt()

        binaryImage.release()

        return whitePixelCount.toDouble() / totalPixels
    }

    /**
     * Match a template image against a row image to detect section numbers.
     * Searches across the entire row to handle cards with section numbers in different positions.
     *
     * @param rowImage Full row image (640xN pixels)
     * @param template Template image of the section number to find
     * @return Match confidence score (0.0 to 1.0, higher = better match)
     */
    private fun matchTemplate(rowImage: Mat, template: Mat): Double {
        require(rowImage.type() == CvType.CV_8UC1) { "Row image must be grayscale" }
        require(template.type() == CvType.CV_8UC1) { "Template must be grayscale" }

        // Perform template matching across entire row
        // Some cards have section numbers on the left, others in the middle
        val result = Mat()
        Imgproc.matchTemplate(rowImage, template, result, Imgproc.TM_CCOEFF_NORMED)

        // Find best match location and score
        val minMaxLoc = org.opencv.core.Core.minMaxLoc(result)
        val matchScore = minMaxLoc.maxVal

        // Clean up
        result.release()

        return matchScore
    }
}
