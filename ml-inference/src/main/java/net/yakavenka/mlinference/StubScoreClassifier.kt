package net.yakavenka.mlinference

import org.opencv.core.Mat

/**
 * Stub implementation of ScoreClassifier for testing infrastructure.
 * Always returns 0 regardless of input.
 * Will be replaced with actual TFLite-based implementation.
 */
class StubScoreClassifier : ScoreClassifier {
    override fun classifyRow(inputMat: Mat): Int {
        // Stub implementation - always return 0
        return 0
    }
}
