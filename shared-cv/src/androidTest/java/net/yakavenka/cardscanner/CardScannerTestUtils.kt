package net.yakavenka.cardscanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

/**
 * Shared test utilities for card scanner components.
 * Extracted to eliminate duplication across multiple test files.
 */
object CardScannerTestUtils {

    /**
     * Load a raw bitmap image from the test assets directory.
     */
    fun loadBitmapFromAssets(filename: String): Bitmap {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return bitmap!!
    }

    /**
     * Load a grayscale Mat from the test assets directory.
     * Converts BGR/RGBA to grayscale for consistent processing.
     */
    fun loadGrayscaleFromAssets(filename: String): Mat {
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

    /**
     * Convert a RowImage (ByteBuffer float32 [0,1]) back to a Mat for comparison/inspection.
     * Dimensions: 66×640 pixels, float32 format.
     */
    fun rowImageToMat(row: RowImage): Mat {
        val mat = Mat(66, 640, CvType.CV_32F)
        row.buffer.rewind()
        val floatArray = FloatArray(66 * 640)
        row.buffer.asFloatBuffer().get(floatArray)
        mat.put(0, 0, floatArray)
        return mat
    }

    /**
     * Convert a RowImage to a ByteBuffer Mat for direct comparison with template matching.
     */
    fun convertRowToByteBuffer(row: RowImage): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(66 * 640 * 4)
        row.buffer.rewind()
        val floatArray = FloatArray(66 * 640)
        row.buffer.asFloatBuffer().get(floatArray)
        buffer.asFloatBuffer().put(floatArray)
        buffer.rewind()
        return buffer
    }
}
