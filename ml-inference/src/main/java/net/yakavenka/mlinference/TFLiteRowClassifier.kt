package net.yakavenka.mlinference

import android.content.Context
import android.util.Log
import net.yakavenka.cardscanner.RowClassifier
import net.yakavenka.cardscanner.RowImage
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * TFLite-backed implementation of RowClassifier.
 * Loads a .tflite model and performs inference on preprocessed row images.
 * Attempts to use GPU delegate for better performance and operation support.
 *
 * Score map: [0→0, 1→1, 2→2, 3→3, 4→5] (Trials scoring uses 0,1,2,3,5; no score of 4)
 */
class TFLiteRowClassifier(context: Context) : RowClassifier {
    private val interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null

    init {
        val modelBuffer = loadModelFile(context, "score_classifier_model.tflite")

        // Try to use GPU delegate for better operation support
        val options = Interpreter.Options()

        try {
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                gpuDelegate = GpuDelegate()
                options.addDelegate(gpuDelegate)
                Log.d(TAG, "Using GPU delegate for TFLite inference")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to create GPU delegate, falling back to CPU", e)
            gpuDelegate = null
        }

        // Set CPU options as fallback
        if (gpuDelegate == null) {
            options.setNumThreads(4)
            Log.d(TAG, "Using CPU with 4 threads for TFLite inference")
        }

        interpreter = Interpreter(modelBuffer, options)
    }

    companion object {
        private const val TAG = "TFLiteRowClassifier"
    }

    override fun classify(row: RowImage): Int {
        // Output buffer: 1×5 float array (5 class probabilities for scores 0,1,2,3,5)
        val outputBuffer = Array(1) { FloatArray(5) }

        // Run inference on the ByteBuffer directly (already float32, normalized [0,1])
        interpreter.run(row.buffer, outputBuffer)

        // Find class with highest probability
        val probabilities = outputBuffer[0]
        val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0

        // Map model output index to actual score (0→0, 1→1, 2→2, 3→3, 4→5)
        // Note: Trials scoring uses 0,1,2,3,5 (no score of 4)
        return if (predictedIndex == 4) 5 else predictedIndex
    }

    private fun loadModelFile(context: Context, modelPath: String): java.nio.MappedByteBuffer {
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
