package net.yakavenka.trialsscore.training

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import nu.pattern.OpenCV
import java.io.File
import kotlin.system.exitProcess

/**
 * Training data preparation tool entry point.
 *
 * Usage:
 *   ./gradlew :training-tool:run --args="--input-dir /path/to/images --output-dir /path/to/output"
 */
fun main(args: Array<String>) {
    val parser = ArgParser("training-data-generator")

    val inputDir by parser.option(
        ArgType.String,
        shortName = "i",
        fullName = "input-dir",
        description = "Input directory containing score card images with 15-digit filenames"
    ).required()

    val outputDir by parser.option(
        ArgType.String,
        shortName = "o",
        fullName = "output-dir",
        description = "Output directory for processed training data"
    ).required()

    try {
        parser.parse(args)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        println()
        println(parser.toString())
        exitProcess(1)
    }

    try {
        // Initialize OpenCV
        println("Loading OpenCV...")
        OpenCV.loadLocally()
        println("OpenCV loaded successfully")
        println()

        // Validate directories
        val input = File(inputDir)
        if (!input.exists() || !input.isDirectory) {
            System.err.println("Error: Input directory does not exist or is not a directory: $inputDir")
            exitProcess(1)
        }

        val output = File(outputDir)

        // Run generator
        val generator = TrainingDataGenerator(input, output)
        generator.process()

        exitProcess(0)
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}
