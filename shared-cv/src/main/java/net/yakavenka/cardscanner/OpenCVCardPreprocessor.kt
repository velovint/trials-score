package net.yakavenka.cardscanner

import android.graphics.Bitmap
import org.opencv.core.Mat

class OpenCVCardPreprocessor(
    private val isolator: CardIsolator = OpenCVCardIsolator(),
    private val segmenter: RowSegmenter = MorphologicalRowSegmenter(),
    private val normalizer: RowNormalizer = OpenCVRowNormalizer(),
) {
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
