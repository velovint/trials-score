package net.yakavenka.cardscanner

import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Stateless image preprocessing utility for score card scanning.
 *
 * Handles:
 * - Image resizing and normalization
 * - Row extraction from score card grid
 */
object CardImagePreprocessor {

    private const val NUM_SECTIONS = 15
    private const val TARGET_WIDTH = 640.0

    /**
     * Preprocess score card image for row extraction.
     * Resizes to 640px width maintaining aspect ratio.
     * @return Preprocessed Mat (caller must release)
     */
    fun preprocessImage(image: Mat): Mat {
        // TODO Phase 4: Implement real preprocessing
        // - Contrast enhancement (CLAHE)
        // - Noise reduction (Gaussian blur)
        // - Rotation correction (perspective transform)

        // Image should already be grayscale from CameraViewModel (CV_8UC1)
        // Resize to standard width for consistent processing
        val resized = Mat()
        val aspectRatio = image.height().toDouble() / image.width().toDouble()
        val targetHeight = (TARGET_WIDTH * aspectRatio).toInt()
        val size = Size(TARGET_WIDTH, targetHeight.toDouble())

        Imgproc.resize(image, resized, size)

        println("Preprocessed image: ${resized.width()}×${resized.height()}, channels=${resized.channels()}")
        return resized
    }

    /**
     * Extract individual row images from preprocessed score card.
     * Divides image into 15 equal-height rows.
     * @return List of 15 row Mats (caller must release all)
     */
    fun extractRowImages(image: Mat): List<Mat> {
        // TODO Phase 4: Implement real grid detection
        // - Edge detection (Canny)
        // - Horizontal line detection (morphological operations)
        // - Line spacing analysis to separate data grid from headers
        // For now: simple equal-height division (sufficient for training data)

        val imageHeight = image.height()
        val imageWidth = image.width()
        val rowHeight = imageHeight / NUM_SECTIONS

        return (0 until NUM_SECTIONS).map { rowIndex ->
            val y = rowIndex * rowHeight
            val height = if (rowIndex == NUM_SECTIONS - 1) {
                imageHeight - y  // Last row: take remaining pixels
            } else {
                rowHeight
            }
            val roi = Rect(0, y, imageWidth, height)
            Mat(image, roi).clone()  // Clone to create independent Mat
        }
    }
}
