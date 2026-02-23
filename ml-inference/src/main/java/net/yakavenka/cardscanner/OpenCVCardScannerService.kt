package net.yakavenka.cardscanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.yakavenka.mlinference.TFLiteRowClassifier

private const val TAG = "OpenCVCardScannerService"

/**
 * Real implementation of CardScannerService using OpenCV + TensorFlow Lite.
 *
 * Coordinates preprocessing and classification:
 * - OpenCVCardPreprocessor (card isolation, row segmentation, normalization)
 * - TFLiteRowClassifier    (TFLite inference, GPU delegate with CPU fallback)
 */
class OpenCVCardScannerService(
    private val context: Context
) : CardScannerService {

    private val classifier = TFLiteRowClassifier(context)
    private val preprocessor = OpenCVCardPreprocessor()

    override suspend fun extractScores(image: Bitmap): ScanResult {
        return withContext(Dispatchers.Default) {
            Log.d(TAG, "Extracting scores from ${image.width}x${image.height} image")
            val rows = runCatching { preprocessor.preprocess(image) }
                .getOrElse { return@withContext ScanResult.Failure(it.message ?: "Scan failed") }
                .getOrElse { return@withContext ScanResult.Failure(it.message ?: "Scan failed") }
            val scores = rows.mapIndexed { i, row ->
                (i + 1) to classifier.classify(row)
            }.toMap()
            ScanResult.Success(scores)
        }
    }

    fun cleanup() {
        classifier.close()
    }
}
