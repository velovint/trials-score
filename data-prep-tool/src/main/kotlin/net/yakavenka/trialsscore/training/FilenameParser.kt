package net.yakavenka.trialsscore.training

/**
 * Parser for score card training data filenames.
 * Extracts 15-digit score sequences from filenames like "IMG_001_012305555000005.jpg"
 */
object FilenameParser {

    private val SCORE_PATTERN = Regex("""(\d{15})""")
    private val VALID_SCORES = setOf(0, 1, 2, 3, 5, 9)  // 9 = missing score marker

    /**
     * Parse 15-digit score sequence from filename.
     * @param filename Name of the file (e.g., "IMG_001_012305555000005.jpg")
     * @return List of 15 scores (0, 1, 2, 3, or 5)
     * @throws IllegalArgumentException if filename doesn't contain valid 15-digit sequence
     */
    fun parseScores(filename: String): List<Int> {
        val match = SCORE_PATTERN.find(filename)
            ?: throw IllegalArgumentException("Filename does not contain 15-digit score sequence: $filename")

        val digitString = match.groupValues[1]
        if (digitString.length != 15) {
            throw IllegalArgumentException("Score sequence must be exactly 15 digits, found ${digitString.length}: $filename")
        }

        val scores = digitString.map { it.digitToInt() }

        // Validate all scores are in valid set
        val invalidScores = scores.filter { it !in VALID_SCORES }
        if (invalidScores.isNotEmpty()) {
            throw IllegalArgumentException("Invalid score values $invalidScores (must be 0, 1, 2, 3, or 5): $filename")
        }

        return scores
    }
}
