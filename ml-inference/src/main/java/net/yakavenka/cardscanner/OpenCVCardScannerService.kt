package net.yakavenka.cardscanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.yakavenka.mlinference.TFLiteRowClassifier
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

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

    override suspend fun extractScores(image: Bitmap): ScanResult {
        // Convert before withContext — Bitmap is immutable, safe across threads
        val rgbaMat = Mat()
        Utils.bitmapToMat(image, rgbaMat)                            // ARGB_8888 → RGBA CV_8UC4
        val grayMat = Mat()
        Imgproc.cvtColor(rgbaMat, grayMat, Imgproc.COLOR_RGBA2GRAY) // → CV_8UC1
        rgbaMat.release()

        return withContext(Dispatchers.Default) {
            Log.d(TAG, "Extracting scores from ${grayMat.width()}x${grayMat.height()} image")
            val pipelineResult = runCatching { pipeline.scan(grayMat) }
                .getOrElse { Result.failure(it) }
            grayMat.release()
            pipelineResult.fold(
                onSuccess = { it },
                onFailure = { ScanResult.Failure(it.message ?: "Scan failed") }
            )
        }
    }

    fun cleanup() {
        rowClassifier.close()
    }
}
