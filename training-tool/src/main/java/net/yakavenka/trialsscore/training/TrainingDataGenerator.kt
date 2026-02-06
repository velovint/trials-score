package net.yakavenka.trialsscore.training

import net.yakavenka.cardscanner.CardImagePreprocessor
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

/**
 * Generates training data from score card images.
 *
 * Processes images with 15-digit score sequences in filenames:
 * - Resizes to 640px width
 * - Extracts 15 individual row images
 * - Organizes by score value into folders (0/, 1/, 2/, 3/, 5/)
 */
class TrainingDataGenerator(
    private val inputDir: File,
    private val outputDir: File
) {

    private val stats = mutableMapOf<Int, Int>()
    private var processedCount = 0
    private var errorCount = 0

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

        val imageFiles = inputDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png")
        } ?: emptyArray()

        println("Found ${imageFiles.size} image files in ${inputDir.absolutePath}")
        println("Processing...")

        imageFiles.forEachIndexed { index, file ->
            try {
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

        // Load image as grayscale
        val image = Imgcodecs.imread(file.absolutePath, Imgcodecs.IMREAD_GRAYSCALE)
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
                        val outputFile = File(outputDir, "$score/image_${imageIndex}_row_${rowIndex}.png")

                        if (Imgcodecs.imwrite(outputFile.absolutePath, rowImage)) {
                            stats[score] = stats.getValue(score) + 1
                        } else {
                            throw IllegalStateException("Failed to write image: ${outputFile.name}")
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
        println()
        println("Rows extracted per score:")
        listOf(0, 1, 2, 3, 5).forEach { score ->
            println("  Score $score: ${stats[score]} rows")
        }
        println()
        println("Output directory: ${outputDir.absolutePath}")
    }
}

private operator fun String.times(n: Int) = repeat(n)
