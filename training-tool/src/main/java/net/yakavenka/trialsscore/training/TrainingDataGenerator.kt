package net.yakavenka.trialsscore.training

import net.yakavenka.cardscanner.CardImagePreprocessor
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

/**
 * Generates training data from score card images.
 *
 * Processes images with 15-digit score sequences in filenames:
 * - Preprocesses full card image
 * - Extracts 15 individual row images
 * - Resizes each row to 640x66 pixels (standard size for ML training)
 * - Organizes by score value into folders (0/, 1/, 2/, 3/, 5/)
 * - Skips rows marked with score 9 (missing/incomplete data)
 */
class TrainingDataGenerator(
    private val inputDir: File,
    private val outputDir: File
) {

    private val stats = mutableMapOf<Int, Int>()
    private var processedCount = 0
    private var errorCount = 0
    private var discardedCount = 0  // Tracks rows with missing scores (marked as 9)

    /**
     * Process all images in input directory.
     */
    fun process() {
        require(inputDir.exists() && inputDir.isDirectory) {
            "Input directory does not exist: ${inputDir.absolutePath}"
        }

        outputDir.mkdirs()

        // Create score subdirectories
        listOf(0, 1, 2, 3, 5).forEach { score ->
            File(outputDir, score.toString()).mkdirs()
            stats[score] = 0
        }

        // Create debug directory
        val debugDir = File(outputDir, "debug")
        debugDir.mkdirs()

        // Enable debug mode for first 3 images
        CardImagePreprocessor.DEBUG_MODE = true
        CardImagePreprocessor.DEBUG_OUTPUT_DIR = debugDir.absolutePath

        val imageFiles = inputDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png")
        } ?: emptyArray()

        println("Found ${imageFiles.size} image files in ${inputDir.absolutePath}")
        println("Debug mode enabled for first 3 images - edge images will be saved to: ${debugDir.absolutePath}")
        println("Processing...")

        imageFiles.forEachIndexed { index, file ->
            try {
                // Disable debug mode after first 3 images
                if (index >= 3) {
                    CardImagePreprocessor.DEBUG_MODE = false
                }

                processImage(file, index)
                processedCount++

                if ((index + 1) % 10 == 0) {
                    println("Processed ${index + 1}/${imageFiles.size} images")
                }
            } catch (e: Exception) {
                errorCount++
                System.err.println("Error processing ${file.name}: ${e.message}")
            }
        }

        printSummary()
    }

    private fun processImage(file: File, imageIndex: Int) {
        // Parse scores from filename
        val scores = FilenameParser.parseScores(file.name)

        // Load image (color or grayscale - preprocessor will handle conversion)
        val image = Imgcodecs.imread(file.absolutePath)
        if (image.empty()) {
            throw IllegalArgumentException("Failed to load image: ${file.name}")
        }

        try {
            // Preprocess and extract rows
            val preprocessed = CardImagePreprocessor.preprocessImage(image)
            try {
                val rows = CardImagePreprocessor.extractRowImages(preprocessed)
                try {
                    // Save each row to appropriate score folder
                    rows.forEachIndexed { rowIndex, rowImage ->
                        val score = scores[rowIndex]

                        // Skip rows with missing scores (marked as 9)
                        if (score == 9) {
                            discardedCount++
                            return@forEachIndexed
                        }

                        // Resize row to standard size for ML training (640x66 pixels)
                        val resized = Mat()
                        Imgproc.resize(rowImage, resized, Size(640.0, 66.0))

                        try {
                            val outputFile = File(outputDir, "$score/image_${imageIndex}_row_${rowIndex}.png")

                            if (Imgcodecs.imwrite(outputFile.absolutePath, resized)) {
                                stats[score] = stats.getValue(score) + 1
                            } else {
                                throw IllegalStateException("Failed to write image: ${outputFile.name}")
                            }
                        } finally {
                            resized.release()
                        }
                    }
                } finally {
                    // Release row Mats
                    rows.forEach { it.release() }
                }
            } finally {
                preprocessed.release()
            }
        } finally {
            image.release()
        }
    }

    private fun printSummary() {
        println()
        println("=" * 50)
        println("Processing Complete")
        println("=" * 50)
        println("Total images processed: $processedCount")
        println("Errors: $errorCount")
        println("Rows discarded (missing scores): $discardedCount")
        println()
        println("Rows extracted per score:")
        listOf(0, 1, 2, 3, 5).forEach { score ->
            println("  Score $score: ${stats[score]} rows")
        }
        val totalSaved = stats.values.sum()
        println()
        println("Total rows saved: $totalSaved")
        println("Total rows discarded: $discardedCount")
        println()
        println("Output directory: ${outputDir.absolutePath}")
    }
}

private operator fun String.times(n: Int) = repeat(n)
