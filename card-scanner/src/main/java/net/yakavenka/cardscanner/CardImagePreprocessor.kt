package net.yakavenka.cardscanner

import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

private const val TAG = "CardImagePreprocessor"

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

        Log.d(TAG, "Preprocessed image: ${resized.width()}×${resized.height()}, channels=${resized.channels()}")
        return resized
    }

    /**
     * Extract individual row images from preprocessed score card.
     * STUB: Returns 15 clones of entire image.
     * @return List of 15 row Mats (caller must release all)
     */
    fun extractRowImages(image: Mat): List<Mat> {
        // TODO Phase 4: Implement real grid detection
        // - Edge detection (Canny)
        // - Horizontal line detection (morphological operations)
        // - Line spacing analysis to separate data grid from headers
        // - Extract 15 individual row images

        // STUB: Return entire image as each "row"
        return (1..NUM_SECTIONS).map { image.clone() }
    }
}
