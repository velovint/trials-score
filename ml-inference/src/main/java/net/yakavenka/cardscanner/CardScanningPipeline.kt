package net.yakavenka.cardscanner

import org.opencv.core.Mat

class CardScanningPipeline(
    private val isolator: CardIsolator,
    private val segmenter: RowSegmenter,
    private val normalizer: RowNormalizer,
    private val classifier: RowClassifier,
) {
    fun scan(image: Mat): Result<ScanResult> {
        TODO()
    }
}
