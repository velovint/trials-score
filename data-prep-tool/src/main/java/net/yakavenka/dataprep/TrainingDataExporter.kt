package net.yakavenka.dataprep

import androidx.test.platform.io.PlatformTestStorage
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs

/**
 * Exporter for training data organized by label folders.
 * Outputs training images to TestStorage for automatic retrieval in build outputs.
 */
object TrainingDataExporter {

    /**
     * Exports training rows to TestStorage organized by label folders.
     * Files are automatically pulled to build/outputs/ after test execution.
     *
     * @param testStorage TestStorage instance from AndroidX Test Services
     * @param rows List of row Mat images to export
     * @param labels List of labels (0-5, or 9 to skip). Must match rows.size
     * @param imageBaseName Base name for output files (e.g., "card_001")
     */
    fun exportToTestStorage(
        testStorage: PlatformTestStorage,
        rows: List<Mat>,
        labels: List<Int>,
        imageBaseName: String
    ) {
        require(rows.size == labels.size) { "Rows and labels must have the same size" }

        rows.forEachIndexed { index, row ->
            val label = labels[index]

            // Skip rows labeled as 9 (corrupted/unclear data)
            if (label == 9) return@forEachIndexed

            // Resize to training dimensions
            val resized = TrainingDataProcessor.prepareRowForTraining(row)

            // Encode Mat to PNG bytes
            val matOfByte = MatOfByte()
            val encodeSuccess = Imgcodecs.imencode(".png", resized, matOfByte)
            require(encodeSuccess) { "Failed to encode Mat to PNG format" }

            val imageBytes = matOfByte.toArray()

            // Write to TestStorage in label-specific folder
            val outputPath = "$label/${imageBaseName}_row_${index}.png"
            testStorage.openOutputFile(outputPath).use { outputStream ->
                outputStream.write(imageBytes)
            }

            // Cleanup
            resized.release()
            matOfByte.release()
        }
    }
}
