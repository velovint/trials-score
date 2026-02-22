package net.yakavenka.cardscanner

import java.nio.ByteBuffer

data class BoundingBox(val x: Int, val y: Int, val width: Int, val height: Int)
data class RowRegion(val top: Int, val bottom: Int)  // Y-coordinates in card space

// Direct ByteBuffer, NativeOrder, float32, normalized [0,1], size = 66 × 640 × 4 bytes
// OpenCV-free after RowNormalizer — usable directly by TFLite Interpreter.run()
@JvmInline value class RowImage(val buffer: ByteBuffer)
