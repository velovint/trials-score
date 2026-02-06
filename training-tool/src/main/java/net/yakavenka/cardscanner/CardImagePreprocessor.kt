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

    // Hough Transform parameters for grid line detection
    private const val HOUGH_THRESHOLD = 50
    private const val MIN_LINE_LENGTH_RATIO = 0.5
    private const val MAX_LINE_GAP = 20.0
    private const val LINE_CLUSTER_THRESHOLD = 5.0
    private const val MAX_LINE_ANGLE_DEGREES = 10.0
    private const val MIN_GRID_LINES = 14

    // Configuration flags
    var ENABLE_CARD_DETECTION = true  // Set to false to skip card detection (if images already cropped)
    var USE_HOUGH_TRANSFORM = true    // Set to false to use equal-height fallback
    var DEBUG_MODE = false            // Set to true to save intermediate images for troubleshooting
    var DEBUG_OUTPUT_DIR: String? = null

    /**
     * Represents a detected horizontal line from Hough Transform.
     */
    private data class HoughLine(val y: Double, val x1: Int, val x2: Int)

    /**
     * Preprocess score card image for row extraction.
     * Converts to grayscale, detects card boundaries (if enabled), crops to card, and resizes to 640px width.
     * @return Preprocessed Mat (caller must release)
     */
    fun preprocessImage(image: Mat): Mat {
        // Step 1: Convert to grayscale if needed
        val grayscale = if (image.channels() == 3) {
            // Color image (BGR) - convert to grayscale
            val gray = Mat()
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
            gray
        } else {
            // Already grayscale
            image
        }

        // Step 2: Detect and crop card boundaries (if enabled)
        val cropped = if (ENABLE_CARD_DETECTION) {
            detectAndCropCard(grayscale)
        } else {
            grayscale
        }

        // Step 3: Resize to standard width for consistent processing
        val resized = Mat()
        val aspectRatio = cropped.height().toDouble() / cropped.width().toDouble()
        val targetHeight = (TARGET_WIDTH * aspectRatio).toInt()
        val size = Size(TARGET_WIDTH, targetHeight.toDouble())

        Imgproc.resize(cropped, resized, size)

        // Clean up intermediate Mats
        // Release grayscale if it was created (color -> gray conversion)
        if (grayscale !== image) {
            grayscale.release()
        }
        // Release cropped if it's different from grayscale
        if (cropped !== grayscale) {
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
     * Detect horizontal lines using enhanced edge detection.
     * Applies Gaussian blur and Canny edge detection, then uses horizontal
     * morphological closing to connect broken line segments.
     */
    private fun detectHorizontalLines(image: Mat): Mat {
        val blurred = Mat()
        val edges = Mat()

        try {
            // Blur to reduce noise
            Imgproc.GaussianBlur(image, blurred, Size(5.0, 5.0), 0.0)

            // Canny edge detection with tuned thresholds
            Imgproc.Canny(blurred, edges, 50.0, 150.0)

            if (DEBUG_MODE && DEBUG_OUTPUT_DIR != null) {
                saveDebugImage(edges, "hough_edges_raw")
            }

            // Morphological operations to enhance horizontal lines
            // Use horizontal kernel to connect broken line segments
            val horizontalKernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                Size(image.width() / 8.0, 1.0)  // Wide horizontal kernel
            )
            Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, horizontalKernel)
            horizontalKernel.release()

            if (DEBUG_MODE && DEBUG_OUTPUT_DIR != null) {
                saveDebugImage(edges, "hough_edges_closed")
            }

            return edges
        } finally {
            blurred.release()
        }
    }

    /**
     * Detect lines using Hough Transform and filter for horizontal lines.
     */
    private fun detectLines(edges: Mat, imageWidth: Int): List<HoughLine> {
        val lines = Mat()

        try {
            // HoughLinesP: Probabilistic Hough Line Transform
            // Parameters tuned for horizontal grid lines
            Imgproc.HoughLinesP(
                edges,
                lines,
                1.0,                                          // rho: Distance resolution: 1 pixel
                Math.PI / 180,                                // theta: Angle resolution: 1 degree
                HOUGH_THRESHOLD,                              // threshold: Min votes to be considered a line
                imageWidth * MIN_LINE_LENGTH_RATIO,           // minLineLength: Min 50% of image width
                MAX_LINE_GAP                                  // maxLineGap: Max gap between line segments
            )

            // Convert to HoughLine objects and filter for horizontal lines
            val detectedLines = mutableListOf<HoughLine>()
            for (i in 0 until lines.rows()) {
                val line = lines.get(i, 0)
                val x1 = line[0].toInt()
                val y1 = line[1].toInt()
                val x2 = line[2].toInt()
                val y2 = line[3].toInt()

                // Filter for nearly horizontal lines (angle < 10 degrees)
                val angle = Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
                if (Math.abs(angle) < Math.toRadians(MAX_LINE_ANGLE_DEGREES)) {
                    val avgY = (y1 + y2) / 2.0
                    detectedLines.add(HoughLine(avgY, x1, x2))
                }
            }

            return detectedLines
        } finally {
            lines.release()
        }
    }

    /**
     * Find the most evenly-spaced subset of lines matching the target count.
     * Searches different windows of lines to find the best uniformity.
     */
    private fun findEvenlySpacedLines(lines: List<Double>, targetCount: Int): List<Double> {
        if (lines.size < targetCount - 2) return emptyList()  // Not enough lines

        // Try different windows of lines to find the most evenly-spaced set
        var bestLines = emptyList<Double>()
        var bestUniformity = Double.MAX_VALUE

        for (startIdx in 0..(lines.size - targetCount + 2)) {
            for (endIdx in (startIdx + targetCount - 2) until lines.size) {
                val window = lines.subList(startIdx, endIdx + 1)
                if (window.size < 14) continue

                // Calculate spacing uniformity (lower is better)
                val spacings = window.zipWithNext { a, b -> b - a }
                val avgSpacing = spacings.average()
                val variance = spacings.map { (it - avgSpacing) * (it - avgSpacing) }.average()
                val uniformity = Math.sqrt(variance) / avgSpacing  // Coefficient of variation

                if (uniformity < bestUniformity) {
                    bestUniformity = uniformity
                    bestLines = window
                }
            }
        }

        if (bestLines.isNotEmpty()) {
            println("  Best grid uniformity: ${"%.3f".format(bestUniformity)} (lower is better)")
        }
        return bestLines
    }

    /**
     * Find grid lines by clustering detected lines and searching for evenly-spaced patterns.
     */
    private fun findGridLines(lines: List<HoughLine>, imageHeight: Int): List<Double> {
        if (lines.isEmpty()) {
            println("  No horizontal lines detected, falling back to equal-height division")
            return emptyList()
        }

        // Sort lines by Y coordinate
        val sortedLines = lines.sortedBy { it.y }

        // Cluster lines that are close together (within LINE_CLUSTER_THRESHOLD pixels)
        val clusteredLines = mutableListOf<Double>()
        var currentCluster = mutableListOf(sortedLines[0].y)

        for (i in 1 until sortedLines.size) {
            val currentY = sortedLines[i].y
            val prevY = sortedLines[i - 1].y

            if (currentY - prevY < LINE_CLUSTER_THRESHOLD) {
                // Same cluster
                currentCluster.add(currentY)
            } else {
                // New cluster - save average of previous cluster
                clusteredLines.add(currentCluster.average())
                currentCluster = mutableListOf(currentY)
            }
        }
        // Don't forget last cluster
        clusteredLines.add(currentCluster.average())

        println("  Detected ${clusteredLines.size} clustered horizontal lines")

        // Find region with 15 evenly-spaced lines (or 16 lines for 15 rows)
        val gridLines = findEvenlySpacedLines(clusteredLines, 16)

        if (gridLines.size >= MIN_GRID_LINES) {
            println("  Found grid with ${gridLines.size} lines (${gridLines.size - 1} rows)")
            return gridLines
        }

        println("  Could not find 15 evenly-spaced lines, falling back to equal-height")
        return emptyList()
    }

    /**
     * Extract rows from image based on detected grid lines.
     */
    private fun extractRowsFromGridLines(image: Mat, gridLines: List<Double>): List<Mat> {
        val rows = mutableListOf<Mat>()

        // Extract rows between consecutive line pairs
        for (i in 0 until (gridLines.size - 1).coerceAtMost(NUM_SECTIONS)) {
            val y1 = gridLines[i].toInt()
            val y2 = gridLines[i + 1].toInt()
            val height = y2 - y1

            if (height > 0) {
                val roi = Rect(0, y1, image.width(), height)
                rows.add(Mat(image, roi).clone())
            }
        }

        println("  Extracted ${rows.size} rows from detected grid lines")

        // If we got fewer than 15 rows, pad with empty rows
        while (rows.size < NUM_SECTIONS) {
            rows.add(Mat(10, image.width(), image.type()))
        }

        return rows.take(NUM_SECTIONS)
    }

    /**
     * Extract rows using equal-height division (fallback method).
     */
    private fun extractRowsEqualHeight(image: Mat): List<Mat> {
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

    /**
     * Extract individual row images from preprocessed score card.
     * Uses Hough Transform to detect grid lines if enabled, otherwise falls back to equal-height division.
     * @return List of 15 row Mats (caller must release all)
     */
    fun extractRowImages(image: Mat): List<Mat> {
        if (!USE_HOUGH_TRANSFORM) {
            println("  Hough Transform disabled, using equal-height division")
            return extractRowsEqualHeight(image)
        }

        val edges = detectHorizontalLines(image)
        val lines = detectLines(edges, image.width())
        val gridLines = findGridLines(lines, image.height())

        // Save debug visualization if enabled
        if (DEBUG_MODE && DEBUG_OUTPUT_DIR != null && gridLines.isNotEmpty()) {
            val debugImg = Mat(image.size(), CvType.CV_8UC3)
            Imgproc.cvtColor(image, debugImg, Imgproc.COLOR_GRAY2BGR)
            gridLines.forEach { y ->
                Imgproc.line(
                    debugImg,
                    Point(0.0, y),
                    Point(image.width().toDouble(), y),
                    Scalar(0.0, 255.0, 0.0),
                    2
                )
            }
            saveDebugImage(debugImg, "detected_grid_lines")
            debugImg.release()
        }

        edges.release()

        // If grid detection succeeded, use detected lines
        if (gridLines.size >= MIN_GRID_LINES) {
            return extractRowsFromGridLines(image, gridLines)
        }

        // Fallback to equal-height division
        println("  Falling back to equal-height row extraction")
        return extractRowsEqualHeight(image)
    }
}
