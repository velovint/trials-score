package net.yakavenka.cardscanner

import org.opencv.core.Mat

fun interface RowSegmenter {
    // Morph cell detection → upside-down correction → Y-cluster
    // Output: 10–16 RowRegions (top/bottom Y coordinates); count depends on card layout and stripHeader setting
    fun segment(card: Mat): Result<List<RowRegion>>
}
