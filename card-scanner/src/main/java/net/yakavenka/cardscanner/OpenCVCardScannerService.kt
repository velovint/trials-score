package net.yakavenka.cardscanner

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "OpenCVCardScannerService"

/**
 * Real implementation of CardScannerService using OpenCV + TensorFlow Lite.
 *
 * Processing pipeline:
 * 1. Preprocess image (placeholder for Phase 3)
 * 2. Extract 15 row images (stubbed - returns entire image as each row)
 * 3. Classify each row with TFLite model
 * 4. Validate and return scores
 */
class OpenCVCardScannerService(
    private val context: Context
) : CardScannerService {

    private lateinit var interpreter: Interpreter

    // TODO: Update based on actual model requirements
    private val modelInputWidth = 640
    private val modelInputHeight = 30
    private val numClasses = 5  // [0, 1, 2, 3, 5]

    companion object {
        private const val MODEL_FILE = "score_classifier_model.tflite"
        private const val NUM_SECTIONS = 15
        private val CLASS_TO_SCORE = intArrayOf(0, 1, 2, 3, 5)
    }

    init {
        initializeModel()
    }

    private fun initializeModel() {
        try {
            // Memory-mapped model loading (TFLite best practice)
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
                // TODO Phase 4+: Add XNNPACK delegate for optimization
                // addDelegate(XNNPackDelegate())
            }

            interpreter = Interpreter(modelBuffer, options)

            // Log model input/output details for debugging
            Log.d(TAG, "Model loaded successfully")
            Log.d(TAG, "Input tensor count: ${interpreter.inputTensorCount}")
            Log.d(TAG, "Output tensor count: ${interpreter.outputTensorCount}")

            if (interpreter.inputTensorCount > 0) {
                val inputTensor = interpreter.getInputTensor(0)
                Log.d(TAG, "Input tensor shape: ${inputTensor.shape().contentToString()}")
                Log.d(TAG, "Input tensor dataType: ${inputTensor.dataType()}")
                Log.d(TAG, "Input tensor name: ${inputTensor.name()}")
            }

            if (interpreter.outputTensorCount > 0) {
                val outputTensor = interpreter.getOutputTensor(0)
                Log.d(TAG, "Output tensor shape: ${outputTensor.shape().contentToString()}")
                Log.d(TAG, "Output tensor dataType: ${outputTensor.dataType()}")
                Log.d(TAG, "Output tensor name: ${outputTensor.name()}")
            }

        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to load TFLite model from assets/$MODEL_FILE. " +
                "Ensure model file exists and is valid.", e
            )
        }
    }

    override suspend fun extractScores(image: Mat): ScanResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Extracting scores from ${image.width()}x${image.height()} grayscale image")

            // Step 1: Preprocess (resize + enhancements)
            val preprocessed = preprocessImage(image)

            // Step 2: Extract rows (STUBBED)
            val rowImages = extractRowImages(preprocessed)
            preprocessed.release()

            // Step 3: Classify each row with TFLite
            val scores = rowImages.mapIndexed { index, rowMat ->
                val score = classifyRow(rowMat)
                rowMat.release()  // Critical: prevent memory leak
                (index + 1) to score  // 1-based section numbering
            }.toMap()

            // Step 4: Validate
            validateScores(scores)

            ScanResult.Success(scores)

        } catch (e: Exception) {
            ScanResult.Failure("Card scanning failed: ${e.toString()} caused by ${e.stackTraceToString()}")
        }
    }

    // PLACEHOLDER: Image preprocessing
    private fun preprocessImage(image: Mat): Mat {
        // TODO Phase 4: Implement real preprocessing
        // - Contrast enhancement (CLAHE)
        // - Noise reduction (Gaussian blur)
        // - Rotation correction (perspective transform)

        // Image should already be grayscale from CameraViewModel (CV_8UC1)
        // Resize to standard width for consistent processing
        val resized = Mat()
        val targetWidth = 640.0
        val aspectRatio = image.height().toDouble() / image.width().toDouble()
        val targetHeight = (targetWidth * aspectRatio).toInt()
        val size = Size(targetWidth, targetHeight.toDouble())

        Imgproc.resize(image, resized, size)

        Log.d(TAG, "Preprocessed image: ${resized.width()}×${resized.height()}, channels=${resized.channels()}")
        return resized
    }

    // STUB: Extract row images (returns entire image 15 times)
    private fun extractRowImages(image: Mat): List<Mat> {
        // TODO Phase 4: Implement real grid detection
        // - Edge detection (Canny)
        // - Horizontal line detection (morphological operations)
        // - Line spacing analysis to separate data grid from headers
        // - Extract 15 individual row images

        // STUB: Return entire image as each "row"
        return (1..NUM_SECTIONS).map { image.clone() }
    }

    // Real ML inference
    private fun classifyRow(rowImage: Mat): Int {
        // Prepare image for model input
        val prepared = prepareForInference(rowImage)

        // Convert to ByteBuffer
        val inputBuffer = matToByteBuffer(prepared)
        prepared.release()

        // Run inference
        val outputArray = Array(1) { FloatArray(numClasses) }
        interpreter.run(inputBuffer, outputArray)

        Log.d(TAG, "Image scanned successfully: $outputArray")
        // Find class with highest probability
        val classIndex = outputArray[0].indices.maxByOrNull {
            outputArray[0][it]
        } ?: throw IllegalStateException("No output from model")

        // Map class index to score value
        return CLASS_TO_SCORE[classIndex]
    }

    /**
     * Resize Mat to format expected by model.
     * Input should already be grayscale from preprocessImage().
     */
    private fun prepareForInference(rowImage: Mat): Mat {
        // Resize to model input dimensions
        val resized = Mat()
        val size = Size(modelInputWidth.toDouble(), modelInputHeight.toDouble())
        Imgproc.resize(rowImage, resized, size)

        return resized
    }

    /**
     * Convert OpenCV Mat to ByteBuffer for TFLite input.
     *
     * Format: NHWC (Batch=1, Height, Width, Channels=1)
     * Data type: Float32 normalized to [0.0, 1.0]
     *
     * TODO: Adjust based on actual model requirements:
     * - Some models use [-1.0, 1.0] normalization
     * - Some models use uint8 inputs
     */
    private fun matToByteBuffer(mat: Mat): ByteBuffer {
        val bufferSize = 1 * modelInputHeight * modelInputWidth * 1 * 4  // NHWC, Float32
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        buffer.order(ByteOrder.nativeOrder())

        // Extract pixel data from Mat
        val matData = ByteArray(mat.total().toInt() * mat.elemSize().toInt())
        mat.get(0, 0, matData)

        // Convert to float and normalize [0, 255] → [0.0, 1.0]
        for (i in matData.indices) {
            val pixelValue = matData[i].toInt() and 0xFF
            val normalizedValue = pixelValue / 255.0f
            buffer.putFloat(normalizedValue)
        }

        buffer.rewind()
        return buffer
    }

    private fun validateScores(scores: Map<Int, Int>) {
        require(scores.size == NUM_SECTIONS) {
            "Expected $NUM_SECTIONS sections, got ${scores.size}"
        }

        scores.forEach { (section, points) ->
            require(section in 1..NUM_SECTIONS) {
                "Invalid section number: $section"
            }
            require(points in setOf(0, 1, 2, 3, 5)) {
                "Invalid score: $points for section $section"
            }
        }
    }

    fun cleanup() {
        interpreter.close()
    }
}
