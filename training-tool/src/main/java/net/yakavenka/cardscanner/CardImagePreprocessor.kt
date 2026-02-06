package net.yakavenka.cardscanner

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.max

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

    // Card detection parameters
    private const val MIN_CARD_AREA_RATIO = 0.1  // Card must be at least 10% of image (lowered)
    private const val CANNY_THRESHOLD1 = 30.0     // Lowered for better edge detection
    private const val CANNY_THRESHOLD2 = 100.0    // Lowered for better edge detection
    private const val GAUSSIAN_BLUR_SIZE = 5

    // Configuration flags
    var ENABLE_CARD_DETECTION = true  // Set to false to skip card detection (if images already cropped)
    var DEBUG_MODE = false            // Set to true to save intermediate images for troubleshooting
    var DEBUG_OUTPUT_DIR: String? = null

    /**
     * Preprocess score card image for row extraction.
     * Detects card boundaries (if enabled), crops to card, and resizes to 640px width.
     * @return Preprocessed Mat (caller must release)
     */
    fun preprocessImage(image: Mat): Mat {
        // Image should already be grayscale from CameraViewModel (CV_8UC1)

        // Step 1: Detect and crop card boundaries (if enabled)
        val cropped = if (ENABLE_CARD_DETECTION) {
            detectAndCropCard(image)
        } else {
            image
        }

        // Step 2: Resize to standard width for consistent processing
        val resized = Mat()
        val aspectRatio = cropped.height().toDouble() / cropped.width().toDouble()
        val targetHeight = (TARGET_WIDTH * aspectRatio).toInt()
        val size = Size(TARGET_WIDTH, targetHeight.toDouble())

        Imgproc.resize(cropped, resized, size)

        // Release cropped if it's different from input
        if (cropped !== image) {
            cropped.release()
        }

        println("Preprocessed image: ${resized.width()}×${resized.height()}, channels=${resized.channels()}")
        return resized
    }

    /**
     * Detect card boundaries and crop to card region.
     * @return Cropped Mat (new Mat if card detected, original if not)
     */
    private fun detectAndCropCard(image: Mat): Mat {
        val blurred = Mat()
        val edges = Mat()
        val closed = Mat()
        val contours = ArrayList<MatOfPoint>()

        try {
            // Apply Gaussian blur to reduce noise
            Imgproc.GaussianBlur(image, blurred, Size(GAUSSIAN_BLUR_SIZE.toDouble(), GAUSSIAN_BLUR_SIZE.toDouble()), 0.0)

            // Detect edges
            Imgproc.Canny(blurred, edges, CANNY_THRESHOLD1, CANNY_THRESHOLD2)

            // Save debug images if enabled
            if (DEBUG_MODE && DEBUG_OUTPUT_DIR != null) {
                saveDebugImage(edges, "edges_raw")
            }

            // Apply morphological closing to connect nearby edges and fill gaps
            // This helps merge small contours into larger card boundary
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(21.0, 21.0))
            Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE, kernel)
            kernel.release()

            if (DEBUG_MODE && DEBUG_OUTPUT_DIR != null) {
                saveDebugImage(closed, "edges_closed")
            }

            // Find contours on the closed edge image
            Imgproc.findContours(closed, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            // Find largest contour that could be the card
            val imageArea = image.width() * image.height()
            val minArea = imageArea * MIN_CARD_AREA_RATIO

            println("  Card detection: imageArea=$imageArea, minArea=$minArea, contours=${contours.size}")

            var largestContour: MatOfPoint? = null
            var largestArea = minArea

            for ((index, contour) in contours.withIndex()) {
                val area = Imgproc.contourArea(contour)
                if (DEBUG_MODE) {
                    println("    Contour $index: area=$area (${(area / imageArea * 100).toInt()}% of image)")
                }
                if (area > largestArea) {
                    largestArea = area
                    largestContour = contour
                }
            }

            // If we found a suitable contour, crop to its bounding rectangle
            if (largestContour != null) {
                val boundingRect = Imgproc.boundingRect(largestContour)

                // Add small padding (2% on each side) to avoid cutting off edges
                val padding = max((boundingRect.width * 0.02).toInt(), (boundingRect.height * 0.02).toInt())
                val paddedRect = Rect(
                    max(0, boundingRect.x - padding),
                    max(0, boundingRect.y - padding),
                    (boundingRect.width + 2 * padding).coerceAtMost(image.width() - max(0, boundingRect.x - padding)),
                    (boundingRect.height + 2 * padding).coerceAtMost(image.height() - max(0, boundingRect.y - padding))
                )

                println("  Card detected: ${paddedRect.width}×${paddedRect.height} at (${paddedRect.x}, ${paddedRect.y}) - ${(largestArea / imageArea * 100).toInt()}% of image")
                return Mat(image, paddedRect).clone()
            } else {
                println("  No card boundary detected (no contour > ${(MIN_CARD_AREA_RATIO * 100).toInt()}% of image), using full image")
                return image
            }

        } finally {
            // Clean up intermediate Mats
            blurred.release()
            edges.release()
            closed.release()
            contours.forEach { it.release() }
        }
    }

    private fun saveDebugImage(mat: Mat, name: String) {
        DEBUG_OUTPUT_DIR?.let { dir ->
            val timestamp = System.currentTimeMillis()
            val path = "$dir/debug_${name}_$timestamp.png"
            org.opencv.imgcodecs.Imgcodecs.imwrite(path, mat)
            println("  Debug image saved: $path")
        }
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
