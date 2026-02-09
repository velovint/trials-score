package net.yakavenka.mlinference

import org.opencv.core.Mat

/**
 * Interface for classifying score card row images using ML model.
 * Returns predicted score value (0-5) for a single row.
 */
interface ScoreClassifier {
    /**
     * Classifies a single score card row image.
     *
     * @param inputMat The preprocessed row image (expected 640x66 grayscale Mat)
     * @return Predicted score value (0-5)
     */
    fun classifyRow(inputMat: Mat): Int
}
