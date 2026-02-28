package net.yakavenka.cardscanner

import android.graphics.Bitmap

/**
 * Service for extracting section scores from score card images.
 *
 * Implementations should process the image asynchronously and return results via ScanResult.
 * The Bitmap image is provided in-memory and may be discarded after extraction is complete.
 */
interface CardScannerService {
    /**
     * Extract section scores from captured score card image.
     *
     * Image is processed in-memory and should be discarded after this call.
     * No image data is persisted to disk.
     *
     * @param image Bitmap of captured score card
     * @return ScanResult.Success with scores map, or Failure with error message
     */
    suspend fun extractScores(image: Bitmap): ScanResult

    /**
     * Release any native resources (e.g. TFLite interpreter, OpenCV allocations).
     * Called when the owning ViewModel is destroyed. No-op by default.
     */
    fun cleanup() {}
}
