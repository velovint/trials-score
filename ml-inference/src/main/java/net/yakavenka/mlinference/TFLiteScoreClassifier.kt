package net.yakavenka.mlinference

import android.content.Context
import android.util.Log
import org.opencv.core.Mat
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite-based implementation of ScoreClassifier.
 * Loads a .tflite model and performs inference on score card row images.
 * Attempts to use GPU delegate for better performance and operation support.
 */
class TFLiteScoreClassifier(context: Context, modelPath: String) : ScoreClassifier {
    private val interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null

    init {
        val modelBuffer = loadModelFile(context, modelPath)

        // Try to use GPU delegate for better operation support
        val compatList = CompatibilityList()
        val options = Interpreter.Options()

        if (compatList.isDelegateSupportedOnThisDevice) {
            try {
                gpuDelegate = GpuDelegate()
                options.addDelegate(gpuDelegate)
                Log.d(TAG, "Using GPU delegate for TFLite inference")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create GPU delegate, falling back to CPU", e)
                gpuDelegate = null
            }
        }

        // Set CPU options as fallback
        if (gpuDelegate == null) {
            options.setNumThreads(4)
            Log.d(TAG, "Using CPU with 4 threads for TFLite inference")
        }

        interpreter = Interpreter(modelBuffer, options)
    }

    companion object {
        private const val TAG = "TFLiteScoreClassifier"
    }

    override fun classifyRow(inputMat: Mat): Int {
        // Prepare input buffer (model expects 640x66 grayscale image)
        val inputBuffer = preprocessMatToBuffer(inputMat)

        // Prepare output buffer (model outputs 5 probabilities for scores 0,1,2,3,5)
        val outputBuffer = Array(1) { FloatArray(5) }

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Find class with highest probability
        val probabilities = outputBuffer[0]
        val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0

        // Map model output index to actual score (0→0, 1→1, 2→2, 3→3, 4→5)
        // Note: Trials scoring uses 0,1,2,3,5 (no score of 4)
        return if (predictedIndex == 4) 5 else predictedIndex
    }

    private fun preprocessMatToBuffer(mat: Mat): ByteBuffer {
        val width = 640
        val height = 66
        val channels = 1 // Grayscale

        // Allocate buffer for input (4 bytes per float)
        val buffer = ByteBuffer.allocateDirect(width * height * channels * 4)
        buffer.order(ByteOrder.nativeOrder())

        // Convert Mat to normalized float values (0.0 - 1.0)
        val data = ByteArray(width * height)
        mat.get(0, 0, data)

        for (pixel in data) {
            val normalizedValue = (pixel.toInt() and 0xFF) / 255.0f
            buffer.putFloat(normalizedValue)
        }

        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter.close()
        gpuDelegate?.close()
    }
}
