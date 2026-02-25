package net.yakavenka.dataprep

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.services.storage.TestStorage
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import net.yakavenka.cardscanner.OpenCVCardIsolator
import net.yakavenka.cardscanner.OpenCVCardPreprocessor
import net.yakavenka.cardscanner.RowImage
import java.io.File

/**
 * Instrumented test to verify real data flow through data-prep-tool.
 * Loads a real score card image, processes it via OpenCVCardPreprocessor,
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

        Log.i("DataPrepRealTest", "OpenCV initialized")
    }

    /**
     * Helper: Convert RowImage (ByteBuffer float32) back to Mat for saving.
     * RowImage buffer is float32 [0,1] at 640×66 — convert back to CV_8U (0-255) for saving.
     */
    private fun rowImageToMat(rowImage: RowImage): Mat {
        val floatBuf = rowImage.buffer.asFloatBuffer()
        val floatArray = FloatArray(floatBuf.capacity())
        floatBuf.get(floatArray)
        rowImage.buffer.rewind()

        // Create float Mat and convert to byte
        val floatMat = Mat(66, 640, CvType.CV_32FC1)
        floatMat.put(0, 0, floatArray)
        val byteMat = Mat()
        floatMat.convertTo(byteMat, CvType.CV_8UC1, 255.0)
        floatMat.release()

        return byteMat
    }

    @Test
    fun processRealScoreCard_extracts15Rows() {
        // Load real score card image from assets
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL001_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val rowImages = OpenCVCardPreprocessor().preprocess(bitmap).getOrThrow()

        // Verify we got 15 rows
        assertThat("Should extract exactly 15 rows from score card", rowImages.size, equalTo(15))

        // Verify rows are properly sized (convert back to Mat to check dimensions)
        rowImages.forEach { rowImage ->
            val mat = rowImageToMat(rowImage)
            assertThat("Row width should be 640px", mat.width(), equalTo(640))
            assertThat("Row height should be 66px", mat.height(), equalTo(66))
            mat.release()
        }

        Log.i("DataPrepRealTest", "Successfully extracted 15 rows from score card")
    }

    @Test
    fun testStorage_savesPreprocessedImage() {
        // Load real score card image from assets
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL001_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Isolate the card
        val isolator = OpenCVCardIsolator()
        val card = isolator.isolate(bitmap).getOrThrow()

        // Save isolated card image using TestStorage
        val testStorage = TestStorage()

        // Save Mat to a temporary file, then copy to TestStorage
        val tempFile = File(context.cacheDir, "preprocessed_test.png")
        val writeSuccess = Imgcodecs.imwrite(tempFile.absolutePath, card)
        assertTrue("Mat should be written to temp file", writeSuccess)

        Log.i("DataPrepRealTest", "Temp file created: ${tempFile.absolutePath}, exists: ${tempFile.exists()}")

        // Use TestStorage to save the output file
        testStorage.openOutputFile("preprocessed_output.png").use { outputStream ->
            tempFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        Log.i("DataPrepRealTest", "File saved to TestStorage: preprocessed_output.png")

        // Verify the isolated card image dimensions
        assertThat("Isolated card width should be 640", card.width(), equalTo(640))
        assertTrue("Isolated card height should be positive", card.height() > 0)

        // Cleanup
        card.release()
        tempFile.delete()

        Log.i("DataPrepRealTest", "Test completed - check build outputs for TestStorage files")
    }

    @Test
    fun prepareRowForTraining_resizesTo640x66() {
        // Load real score card image from assets
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inputStream = context.assets.open("raw/PXL001_100112010299999.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()


        val rowImages = OpenCVCardPreprocessor().preprocess(bitmap).getOrThrow()

        // Verify all rows are already 640x66 (normalized by OpenCVRowNormalizer)
        rowImages.forEach { rowImage ->
            val mat = rowImageToMat(rowImage)
            assertThat("Row width should be 640", mat.width(), equalTo(640))
            assertThat("Row height should be 66", mat.height(), equalTo(66))
            mat.release()
        }

        // Verify we still have 15 rows
        assertThat("Should have 15 rows", rowImages.size, equalTo(15))

        Log.i("DataPrepRealTest", "All 15 rows verified at 640x66 resolution")
    }

    @Test
    fun exportTrainingData_organizesIntoLabelFolders() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testStorage = TestStorage()
        val assetFiles = context.assets.list("raw") ?: emptyArray()

        for (filename in assetFiles) {
            // Parse: "PXL001_100112010299999.jpg" → base="PXL001", labelStr="100112010299999"
            val nameWithoutExt = filename.substringBeforeLast(".")
            val parts = nameWithoutExt.split("_")
            if (parts.size < 2) {
                Log.w("DataPrepRealTest", "Skipping $filename: filename does not match pattern")
                continue
            }

            val imageBaseName = parts[0]
            val labelStr = parts[1]
            val labels = labelStr.map { it.digitToInt() }

            // Load image from assets
            val bitmap = context.assets.open("raw/$filename").use {
                BitmapFactory.decodeStream(it)
            }

            // Preprocess and check for errors
            val result = OpenCVCardPreprocessor().preprocess(bitmap)
            if (result.isFailure) {
                Log.w("DataPrepRealTest", "Skipping $filename: ${result.exceptionOrNull()?.message}")
                continue
            }

            val rowImages = result.getOrThrow()
            assertThat("Detected 15 rows [$filename]", rowImages.size, equalTo(15))

            // Convert RowImages back to Mat for export (TrainingDataExporter expects Mat)
            val rowMats = rowImages.map { rowImageToMat(it) }

            // Export rows to TestStorage organized by label folders
            TrainingDataExporter.exportToTestStorage(
                testStorage = testStorage,
                rows = rowMats,
                labels = labels,
                imageBaseName = imageBaseName
            )

            rowMats.forEach { it.release() }

            Log.i("DataPrepRealTest", "Exported $filename (${rowImages.size} rows)")
        }

        Log.i("DataPrepRealTest", "Training data exported to TestStorage")
        Log.i("DataPrepRealTest", "Check build/outputs/managed_device_android_test_additional_output/ for exported files")
    }
}
