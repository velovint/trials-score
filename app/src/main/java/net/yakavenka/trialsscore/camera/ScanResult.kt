package net.yakavenka.trialsscore.camera

/**
 * Result of extracting scores from a captured score card image.
 */
sealed class ScanResult {
    /**
     * Successfully extracted scores from the image.
     * @param scores Map of section number (1-based) to points (0-5)
     */
    data class Success(val scores: Map<Int, Int>) : ScanResult()

    /**
     * Failed to extract scores from the image.
     * @param error Description of what went wrong
     */
    data class Failure(val error: String) : ScanResult()
}
