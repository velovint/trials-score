package net.yakavenka.mlinference

import android.content.Context
import android.util.Log
import net.yakavenka.cardscanner.RowClassifier
import net.yakavenka.cardscanner.RowImage
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * TFLite-backed implementation of RowClassifier.
 * Loads a .tflite model and performs inference on preprocessed row images.
 *
 * Score map: [0→0, 1→1, 2→2, 3→3, 4→5] (Trials scoring uses 0,1,2,3,5; no score of 4)
 */
class TFLiteRowClassifier(context: Context) : RowClassifier, Closeable {
    private val interpreter: Interpreter

    init {
        val modelBuffer = loadModelFile(context, "score_classifier_model.tflite")
        val options = Interpreter.Options().apply { setNumThreads(4) }
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
        return context.assets.openFd(modelPath).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                inputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }
    }
}
