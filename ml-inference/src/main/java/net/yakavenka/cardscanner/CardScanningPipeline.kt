package net.yakavenka.cardscanner

import android.graphics.Bitmap
import org.opencv.core.Mat

class CardScanningPipeline(
    private val isolator: CardIsolator,
    private val segmenter: RowSegmenter,
    private val normalizer: RowNormalizer,
    private val classifier: RowClassifier,
) {
    fun scan(image: Bitmap): Result<ScanResult> {
        val card    = isolator.isolate(image).getOrElse { return Result.failure(it) }
        val regions = segmenter.segment(card).getOrElse { return Result.failure(it) }
        val rows    = normalizer.normalize(card, regions)
        val scores  = rows.mapIndexed { i, row -> (i + 1) to classifier.classify(row) }.toMap()
        return Result.success(ScanResult.Success(scores))
    }
}
