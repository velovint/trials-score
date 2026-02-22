package net.yakavenka.cardscanner

import org.opencv.core.Mat

fun interface RowSegmenter {
    // Morph cell detection → upside-down correction → Y-cluster
    // Output: 15 RowRegions (top/bottom Y coordinates)
    fun segment(card: Mat): Result<List<RowRegion>>
}
