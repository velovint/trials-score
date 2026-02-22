package net.yakavenka.cardscanner

import org.opencv.core.Mat

fun interface RowNormalizer {
    // Crops each region from card at full width, resizes to 640×66 px,
    // converts to float32 normalized [0,1], packs into a direct ByteBuffer
    // No failure modes — dimensions are validated upstream
    fun normalize(card: Mat, regions: List<RowRegion>): List<RowImage>
}
