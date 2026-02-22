package net.yakavenka.cardscanner

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.yakavenka.mlinference.TFLiteRowClassifier
import org.opencv.core.Mat

private const val TAG = "OpenCVCardScannerService"

/**
 * Real implementation of CardScannerService using OpenCV + TensorFlow Lite.
 *
 * Delegates to CardScanningPipeline composed of:
 * - OpenCVCardIsolator     (Canny edges → contour crop → portrait orientation → 640px resize)
 * - MorphologicalRowSegmenter (adaptive threshold → morph open → contour filter → Y-cluster)
 * - OpenCVRowNormalizer    (crop, resize to 640×66, float32 [0,1])
 * - TFLiteRowClassifier    (TFLite inference, GPU delegate with CPU fallback)
 */
class OpenCVCardScannerService(
    private val context: Context
) : CardScannerService {

    private val rowClassifier = TFLiteRowClassifier(context)
    private val pipeline = CardScanningPipeline(
        isolator   = OpenCVCardIsolator(),
        segmenter  = MorphologicalRowSegmenter(),
        normalizer = OpenCVRowNormalizer(),
        classifier = rowClassifier,
    )

    override suspend fun extractScores(image: Mat): ScanResult = withContext(Dispatchers.Default) {
        Log.d(TAG, "Extracting scores from ${image.width()}x${image.height()} image")
        val pipelineResult = runCatching { pipeline.scan(image) }
            .getOrElse { Result.failure(it) }
        pipelineResult.fold(
            onSuccess = { it },
            onFailure = { ScanResult.Failure(it.message ?: "Scan failed") }
        )
    }

    fun cleanup() {
        rowClassifier.close()
    }
}
