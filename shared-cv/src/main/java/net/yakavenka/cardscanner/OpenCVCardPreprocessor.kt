package net.yakavenka.cardscanner

import android.graphics.Bitmap
import org.opencv.core.Mat

class OpenCVCardPreprocessor(
    private val isolator: CardIsolator,
    private val segmenter: RowSegmenter,
    private val normalizer: RowNormalizer,
) {
    constructor(debugObserver: ScanDebugObserver = ScanDebugObserver.NO_OP) : this(
        isolator = OpenCVCardIsolator(debugObserver = debugObserver),
        segmenter = MorphologicalRowSegmenter(debugObserver = debugObserver),
        normalizer = OpenCVRowNormalizer(debugObserver = debugObserver),
    )

    fun preprocess(image: Bitmap): Result<List<RowImage>> {
        val card = isolator.isolate(image).getOrElse { return Result.failure(it) }
        val regions = segmenter.segment(card).getOrElse {
            card.release()
            return Result.failure(it)
        }
        val rows = normalizer.normalize(card, regions)
        card.release()
        return Result.success(rows)
    }
}
