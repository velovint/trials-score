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
 * - OpenCVCardIsolator (stub: pass-through)
 * - MorphologicalRowSegmenter (stub: equal-height rows)
 * - OpenCVRowNormalizer (fully implemented: crop, resize to 640×66, float32 [0,1])
 * - TFLiteRowClassifier (stub: returns 0; real inference wired in Slice 4.3)
 */
class OpenCVCardScannerService(
    private val context: Context
) : CardScannerService {

    private val pipeline = CardScanningPipeline(
        isolator   = OpenCVCardIsolator(),
        segmenter  = MorphologicalRowSegmenter(),
        normalizer = OpenCVRowNormalizer(),
        classifier = TFLiteRowClassifier(),
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
        // TODO Phase 5: release any held resources (e.g. TFLite interpreter)
    }
}
