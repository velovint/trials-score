package net.yakavenka.cardscanner

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.max

class OpenCVCardIsolator(
    private val targetWidth: Int? = 640
) : CardIsolator {

    companion object {
        private const val MIN_CARD_AREA_RATIO = 0.15
        private const val CANNY_THRESHOLD1 = 50.0
        private const val CANNY_THRESHOLD2 = 150.0
        private const val GAUSSIAN_BLUR_SIZE = 5
        private const val MIN_PORTRAIT_ASPECT_RATIO = 1.5
    }

    override fun isolate(image: Bitmap): Result<Mat> {
        try {
            // Step 0: Convert Bitmap to grayscale Mat
            val rgbaMat = Mat()
            Utils.bitmapToMat(image, rgbaMat)                            // ARGB_8888 → RGBA CV_8UC4
            val grayscale = Mat()
            Imgproc.cvtColor(rgbaMat, grayscale, Imgproc.COLOR_RGBA2GRAY) // → CV_8UC1
            rgbaMat.release()

            // Step 1: Auto-rotate to portrait if landscape (width > height)
            val oriented = if (grayscale.width() > grayscale.height()) {
                Log.i("OpenCVCardIsolator", "Rotating landscape to portrait: ${grayscale.width()}x${grayscale.height()}")
                val rotated = Mat()
                Core.rotate(grayscale, rotated, Core.ROTATE_90_COUNTERCLOCKWISE)
                grayscale.release()
                rotated
            } else {
                grayscale
            }

            // Step 2: Card boundary detection
            val card = detectAndCropCard(oriented)

            // Release oriented if it was created from rotation
            if (oriented !== grayscale) {
                oriented.release()
            }

            return card.fold(
                onSuccess = { cardMat ->
                    // Continue to Step 3: Optionally resize to target width
                    val output = if (targetWidth != null) {
                        val resized = Mat()
                        val aspectRatio = cardMat.height().toDouble() / cardMat.width()
                        Imgproc.resize(cardMat, resized, Size(targetWidth.toDouble(), targetWidth * aspectRatio))
                        cardMat.release()
                        resized
                    } else {
                        cardMat   // return at natural crop resolution
                    }

                    Log.i("OpenCVCardIsolator", "Card isolated: ${output.width()}×${output.height()}")
                    Result.success(output)
                },
                onFailure = { error ->
                    if (oriented !== grayscale) {
                        grayscale.release()
                    }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e("OpenCVCardIsolator", "Error isolating card", e)
            return Result.failure(ScanError.CardNotFound)
        }
    }

    private fun detectAndCropCard(image: Mat): Result<Mat> {
        val blurred = Mat()
        val edges = Mat()
        val closed = Mat()
        val contours = ArrayList<MatOfPoint>()

        return try {
            // Apply Gaussian blur to reduce noise
            Imgproc.GaussianBlur(image, blurred, Size(GAUSSIAN_BLUR_SIZE.toDouble(), GAUSSIAN_BLUR_SIZE.toDouble()), 0.0)

            // Detect edges
            Imgproc.Canny(blurred, edges, CANNY_THRESHOLD1, CANNY_THRESHOLD2)

            // Apply morphological closing to connect nearby edges and fill gaps
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 15.0))
            Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE, kernel)
            kernel.release()

            // Find contours on the closed edge image
            Imgproc.findContours(closed, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            // Find largest contour that could be the card
            val imageArea = image.width() * image.height()
            val minArea = imageArea * MIN_CARD_AREA_RATIO

            Log.i("OpenCVCardIsolator", "Card detection: imageArea=$imageArea, minArea=$minArea, contours=${contours.size}")

            var largestContour: MatOfPoint? = null
            var largestArea = minArea

            for ((index, contour) in contours.withIndex()) {
                val area = Imgproc.contourArea(contour)
                Log.i("OpenCVCardIsolator", "  Contour $index: area=$area (${(area / imageArea * 100).toInt()}% of image)")
                if (area > largestArea) {
                    largestArea = area
                    largestContour = contour
                }
            }

            // If no suitable contour found, return failure
            if (largestContour == null) {
                Log.i("OpenCVCardIsolator", "No card boundary detected (no contour > ${(MIN_CARD_AREA_RATIO * 100).toInt()}% of image)")
                return Result.failure(ScanError.CardNotFound)
            }

            val boundingRect = Imgproc.boundingRect(largestContour)

            // Validation: Check if detected region makes sense
            val aspectRatio = boundingRect.height.toDouble() / boundingRect.width.toDouble()
            val areaRatio = largestArea / imageArea
            val isReasonableSize = areaRatio < 0.9  // Not almost entire image
            val isPortraitish = aspectRatio > MIN_PORTRAIT_ASPECT_RATIO  // Score cards are tall

            Log.i("OpenCVCardIsolator", "  Validation: rect=(${boundingRect.x}, ${boundingRect.y}, ${boundingRect.width}×${boundingRect.height})")
            Log.i("OpenCVCardIsolator", "  aspectRatio=${"%.2f".format(aspectRatio)}, areaRatio=${"%.2f".format(areaRatio)}")
            Log.i("OpenCVCardIsolator", "  isReasonableSize=$isReasonableSize, isPortraitish=$isPortraitish")

            // Check aspect ratio first
            if (!isPortraitish) {
                Log.i("OpenCVCardIsolator", "Card detection rejected: Invalid aspect ratio ${aspectRatio.toFloat()}")
                return Result.failure(ScanError.InvalidAspectRatio(aspectRatio.toFloat()))
            }

            val isValidDetection = isReasonableSize && isPortraitish

            if (!isValidDetection) {
                Log.i("OpenCVCardIsolator", "Card detection rejected: Invalid dimensions")
                return Result.failure(ScanError.CardNotFound)
            }

            // Add small padding (2% on each side) to avoid cutting off edges
            val padding = max((boundingRect.width * 0.02).toInt(), (boundingRect.height * 0.02).toInt())
            val paddedRect = Rect(
                max(0, boundingRect.x - padding),
                max(0, boundingRect.y - padding),
                (boundingRect.width + 2 * padding).coerceAtMost(image.width() - max(0, boundingRect.x - padding)),
                (boundingRect.height + 2 * padding).coerceAtMost(image.height() - max(0, boundingRect.y - padding))
            )

            Log.i("OpenCVCardIsolator", "Card detected: ${paddedRect.width}×${paddedRect.height} at (${paddedRect.x}, ${paddedRect.y})")
            Result.success(Mat(image, paddedRect).clone())

        } finally {
            // Clean up intermediate Mats
            blurred.release()
            edges.release()
            closed.release()
            contours.forEach { it.release() }
        }
    }
}
