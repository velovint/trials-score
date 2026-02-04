package net.yakavenka.trialsscore.camera

import android.graphics.Bitmap

/**
 * Service for extracting section scores from score card images.
 *
 * Implementations should process the bitmap asynchronously and return results via ScanResult.
 * The bitmap is provided in-memory and may be discarded after extraction is complete.
 */
interface CardScannerService {
    /**
     * Extract section scores from captured score card image.
     *
     * Image is processed in-memory and should be discarded after this call.
     * No image data is persisted to disk.
     *
     * @param bitmap In-memory bitmap of captured score card
     * @return ScanResult.Success with scores map, or Failure with error message
     */
    suspend fun extractScores(bitmap: Bitmap): ScanResult
}
