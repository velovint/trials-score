package net.yakavenka.cardscanner

import android.graphics.Bitmap
import org.opencv.core.Mat

fun interface CardIsolator {
    // Accepts Bitmap; converts to grayscale Mat internally
    // Canny edges → quad contour → perspective warp → rotate if sideways
    // Output: portrait-oriented card at 640px width
    fun isolate(image: Bitmap): Result<Mat>
}
