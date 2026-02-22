package net.yakavenka.cardscanner

import org.opencv.core.Mat

fun interface CardIsolator {
    // Canny edges → quad contour → perspective warp → rotate if sideways
    // Output: portrait-oriented card at 640px width
    fun isolate(image: Mat): Result<Mat>
}
