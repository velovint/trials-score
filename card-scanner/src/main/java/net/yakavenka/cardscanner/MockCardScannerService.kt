package net.yakavenka.cardscanner

import kotlinx.coroutines.delay
import org.opencv.core.Mat

/**
 * Mock implementation of CardScannerService for testing and development.
 *
 * Returns hardcoded test scores to simulate score card processing.
 * Adds a 500ms delay to simulate real CV processing time.
 */
class MockCardScannerService : CardScannerService {

    override suspend fun extractScores(image: Mat): ScanResult {
        // Simulate CV processing delay
        delay(500)

        // Return hardcoded test scores
        // Pattern: [0, 1, 0, 2, 0, 3, 0, 5, 0, 1, 0, 0]
        val scores = mapOf(
            1 to 0,
            2 to 1,
            3 to 0,
            4 to 2,
            5 to 0,
            6 to 3,
            7 to 0,
            8 to 5,
            9 to 0,
            10 to 1,
            11 to 0,
            12 to 0
        )

        return ScanResult.Success(scores)
    }
}
