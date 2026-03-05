package net.yakavenka.mlinference

import android.content.Context
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import net.yakavenka.cardscanner.RowClassifier
import net.yakavenka.cardscanner.RowImage
import java.io.Closeable

/**
 * LiteRT CompiledModel-backed implementation of RowClassifier.
 * Loads a .tflite model and performs CPU inference on preprocessed row images.
 *
 * Score map: [0→0, 1→1, 2→2, 3→3, 4→5] (Trials scoring uses 0,1,2,3,5; no score of 4)
 */
class TFLiteRowClassifier(context: Context) : RowClassifier, Closeable {
    private val model = CompiledModel.create(
        context.assets,
        "score_classifier_model.tflite",
        CompiledModel.Options(Accelerator.CPU),
        null
    )
    private val inputBuffers = model.createInputBuffers()
    private val outputBuffers = model.createOutputBuffers()

    companion object {
        private const val INPUT_SIZE = 640 * 66
    }

    override fun classify(row: RowImage): Int {
        // Extract float values from RowImage ByteBuffer (float32, normalized [0,1])
        val inputFloats = FloatArray(INPUT_SIZE)
        row.buffer.rewind()
        row.buffer.asFloatBuffer().get(inputFloats)

        inputBuffers[0].writeFloat(inputFloats)
        model.run(inputBuffers, outputBuffers)

        // Read class probabilities (5 classes: scores 0, 1, 2, 3, 5)
        val probabilities = outputBuffers[0].readFloat()
        val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0

        // Map model output index to actual score (0→0, 1→1, 2→2, 3→3, 4→5)
        // Note: Trials scoring uses 0,1,2,3,5 (no score of 4)
        return if (predictedIndex == 4) 5 else predictedIndex
    }

    override fun close() {
        inputBuffers.forEach { it.close() }
        outputBuffers.forEach { it.close() }
        model.close()
    }
}
