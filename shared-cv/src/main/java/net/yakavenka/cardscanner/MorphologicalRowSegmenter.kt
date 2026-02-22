package net.yakavenka.cardscanner

import org.opencv.core.Mat

class MorphologicalRowSegmenter : RowSegmenter {
    override fun segment(card: Mat): Result<List<RowRegion>> {
        val rowHeight = card.rows() / 15
        return Result.success(
            List(15) { i -> RowRegion(i * rowHeight, (i + 1) * rowHeight) }
        )
    }
}
