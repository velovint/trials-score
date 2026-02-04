package net.yakavenka.cardscanner

import org.opencv.core.Mat

/**
 * Service for extracting section scores from score card images.
 *
 * Implementations should process the image asynchronously and return results via ScanResult.
 * The Mat image is provided in-memory and may be discarded after extraction is complete.
 */
interface CardScannerService {
    /**
     * Extract section scores from captured score card image.
     *
     * Image is processed in-memory and should be discarded after this call.
     * No image data is persisted to disk.
     *
     * @param image In-memory OpenCV Mat of captured score card
     * @return ScanResult.Success with scores map, or Failure with error message
     */
    suspend fun extractScores(image: Mat): ScanResult
}
