package net.yakavenka.dataprep

import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Processor for preparing row images for training.
 * Handles resizing extracted rows to standardized dimensions for ML model input.
 */
object TrainingDataProcessor {
    private const val TARGET_ROW_WIDTH = 640
    private const val TARGET_ROW_HEIGHT = 66

    /**
     * Prepares a row Mat for training by resizing to standardized dimensions.
     *
     * @param rowMat The input row Mat from CardImagePreprocessor
     * @return A new Mat resized to 640x66 pixels. Caller is responsible for releasing.
     */
    fun prepareRowForTraining(rowMat: Mat): Mat {
        val resized = Mat()
        val size = Size(TARGET_ROW_WIDTH.toDouble(), TARGET_ROW_HEIGHT.toDouble())
        Imgproc.resize(rowMat, resized, size)
        return resized
    }
}
