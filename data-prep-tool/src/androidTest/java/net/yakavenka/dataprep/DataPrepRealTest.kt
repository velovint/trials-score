package net.yakavenka.dataprep

import android.graphics.BitmapFactory
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import net.yakavenka.cardscanner.CardImagePreprocessor
import android.util.Log
import androidx.test.rule.GrantPermissionRule
import androidx.test.services.storage.TestStorage
import org.junit.Rule
import java.io.File
import java.io.FileOutputStream
import org.opencv.imgcodecs.Imgcodecs

/**
 * Instrumented test to verify real data flow through data-prep-tool.
 * Loads a real score card image, processes it with CardImagePreprocessor,
 * and verifies row extraction works correctly.
 */
@RunWith(AndroidJUnit4::class)
class DataPrepRealTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Before
    fun setUp() {
        // Initialize OpenCV native library
        OpenCVLoader.initLocal()

        // Set debug output to test app's external files directory (accessible via adb pull)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // This gets /storage/emulated/0/Android/data/
        val baseDir = Environment.getExternalStorageDirectory()
        val mainPackageName = "net.yakavenka.dataprep"
//        val debugDir = context.getExternalFilesDir(null)!!.resolve("card-debug")
        val debugDir = File(baseDir, "Android/data/$mainPackageName/files/card-debug")
        debugDir.let {
            if (!it.exists()) {
                val created = it.mkdirs()
                Log.d("OpenCV_Test", "Directory created: $created")
            }
        }

        CardImagePreprocessor.DEBUG_OUTPUT_DIR = debugDir.absolutePath
        CardImagePreprocessor.DEBUG_MODE = true

        Log.i("DataPrepRealTest", "Debug output directory: ${debugDir.absolutePath}")
        Log.i("DataPrepRealTest", "Directory exists: ${debugDir.exists()}")
    }

    @Test
    fun processRealScoreCard_extracts15Rows() {
        // Load real score card image from assets
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale (CardImagePreprocessor expects grayscale)
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        // Use real CardImagePreprocessor
        val preprocessed = CardImagePreprocessor.preprocessImage(grayMat)
        val rows = CardImagePreprocessor.extractRowImages(preprocessed)

        // Verify we got 15 rows
        assertThat("Should extract exactly 15 rows from score card", rows.size, equalTo(15))

        // Verify rows are properly sized (640px wide)
        rows.forEach { row ->
            assertThat("Row width should be 640px", row.width(), equalTo(640))
            assertTrue("Row height should be positive", row.height() > 0)
        }

        // Cleanup
        grayMat.release()
        preprocessed.release()
        rows.forEach { it.release() }
//        Thread.sleep(15000);
    }

    @Test
    fun testStorage_savesPreprocessedImage() {
        // Load real score card image from assets
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        // Preprocess the image
        val preprocessed = CardImagePreprocessor.preprocessImage(grayMat)
        grayMat.release()

        // Save preprocessed image using TestStorage
        val testStorage = TestStorage()

        // First save Mat to a temporary file, then copy to TestStorage
        val tempFile = File(context.cacheDir, "preprocessed_test.png")
        val writeSuccess = Imgcodecs.imwrite(tempFile.absolutePath, preprocessed)
        assertTrue("Mat should be written to temp file", writeSuccess)

        Log.i("DataPrepRealTest", "Temp file created: ${tempFile.absolutePath}, exists: ${tempFile.exists()}")

        // Use TestStorage to save the output file
        testStorage.openOutputFile("preprocessed_output.png").use { outputStream ->
            tempFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        Log.i("DataPrepRealTest", "File saved to TestStorage: preprocessed_output.png")

        // Verify the preprocessed image dimensions
        assertThat("Preprocessed width should be 640", preprocessed.width(), equalTo(640))
        assertTrue("Preprocessed height should be positive", preprocessed.height() > 0)

        // Cleanup
        preprocessed.release()
        tempFile.delete()

        Log.i("DataPrepRealTest", "Test completed - check build outputs for TestStorage files")
    }

    @Test
    fun prepareRowForTraining_resizesTo640x66() {
        // Load real score card image from assets
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        // Extract rows using CardImagePreprocessor
        val preprocessed = CardImagePreprocessor.preprocessImage(grayMat)
        val rows = CardImagePreprocessor.extractRowImages(preprocessed)
        grayMat.release()
        preprocessed.release()

        // Resize each row for training
        val resizedRows = rows.map { TrainingDataProcessor.prepareRowForTraining(it) }

        // Verify all resized rows have correct dimensions
        resizedRows.forEach { row ->
            assertThat("Resized row width should be 640", row.width(), equalTo(640))
            assertThat("Resized row height should be 66", row.height(), equalTo(66))
        }

        // Verify we still have 15 rows
        assertThat("Should have 15 resized rows", resizedRows.size, equalTo(15))

        // Cleanup
        rows.forEach { it.release() }
        resizedRows.forEach { it.release() }
    }

    @Test
    fun exportTrainingData_organizesIntoLabelFolders() {
        // Load real score card image from assets
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Convert to Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        // Process through CardImagePreprocessor to extract 15 rows
        val preprocessed = CardImagePreprocessor.preprocessImage(grayMat)
        val rows = CardImagePreprocessor.extractRowImages(preprocessed)
        grayMat.release()
        preprocessed.release()

        // Simulated labels (in real workflow, user provides these)
        // Label 9 indicates "skip this row" (corrupted/unclear data)
        val labels = listOf(1, 0, 0, 1, 1, 2, 0, 1, 0, 2, 9, 9, 9, 9, 9,)

        // Create TestStorage instance
        val testStorage = TestStorage()

        // Export rows to TestStorage organized by label folders
        TrainingDataExporter.exportToTestStorage(
            testStorage = testStorage,
            rows = rows,
            labels = labels,
            imageBaseName = "test_card_001"
        )

        Log.i("DataPrepRealTest", "Training data exported to TestStorage")
        Log.i("DataPrepRealTest", "Check build/outputs/managed_device_android_test_additional_output/ for exported files")

        // Cleanup
        rows.forEach { it.release() }
    }
}
