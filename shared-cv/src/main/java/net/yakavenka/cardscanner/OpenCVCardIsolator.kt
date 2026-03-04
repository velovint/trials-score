package net.yakavenka.cardscanner

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.max

class OpenCVCardIsolator(
    private val targetWidth: Int? = 640,
    private val debugObserver: ScanDebugObserver = ScanDebugObserver.NO_OP
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
            val wasRotated = grayscale.width() > grayscale.height()
            val oriented = if (wasRotated) {
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

            // Release oriented now — detectAndCropCard always clones its result
            oriented.release()

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

                    debugObserver.onImage("01_card_boundary.png", output)
                    Log.i("OpenCVCardIsolator", "Card isolated: ${output.width()}×${output.height()}")
                    Result.success(output)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e("OpenCVCardIsolator", "Error isolating card", e)
            return Result.failure(ScanError.CardNotFound)
        }
    }

    private data class ContourMatch(val rect: Rect, val contourArea: Double)

    private fun detectAndCropCard(image: Mat): Result<Mat> {
        val imageArea = image.width().toDouble() * image.height()

        val edgeMap = buildEdgeMap(image)
        val match = findLargestContourRect(edgeMap, imageArea)
        edgeMap.release()

        if (match == null) {
            Log.i("OpenCVCardIsolator", "No card boundary detected (no contour > ${(MIN_CARD_AREA_RATIO * 100).toInt()}% of image)")
            return Result.failure(ScanError.CardNotFound)
        }

        val validation = validateRect(match.rect, match.contourArea, imageArea)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        return Result.success(cropWithPadding(image, match.rect))
    }

    private fun buildEdgeMap(image: Mat): Mat {
        val blurred = Mat()
        val edges = Mat()

        Imgproc.GaussianBlur(image, blurred, Size(GAUSSIAN_BLUR_SIZE.toDouble(), GAUSSIAN_BLUR_SIZE.toDouble()), 0.0)
        Imgproc.Canny(blurred, edges, CANNY_THRESHOLD1, CANNY_THRESHOLD2)
        blurred.release()

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 15.0))
        val closed = Mat()
        Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE, kernel)
        kernel.release()
        edges.release()

        return closed
    }

    private fun findLargestContourRect(edgeMap: Mat, imageArea: Double): ContourMatch? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edgeMap, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()

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

        val result = largestContour?.let { ContourMatch(Imgproc.boundingRect(it), largestArea) }
        contours.forEach { it.release() }
        return result
    }

    private fun validateRect(rect: Rect, contourArea: Double, imageArea: Double): Result<Unit> {
        val aspectRatio = rect.height.toDouble() / rect.width.toDouble()
        val areaRatio = contourArea / imageArea
        val isReasonableSize = areaRatio < 0.9
        val isPortraitish = aspectRatio > MIN_PORTRAIT_ASPECT_RATIO

        Log.i("OpenCVCardIsolator", "  Validation: rect=(${rect.x}, ${rect.y}, ${rect.width}×${rect.height})")
        Log.i("OpenCVCardIsolator", "  aspectRatio=${"%.2f".format(aspectRatio)}, areaRatio=${"%.2f".format(areaRatio)}")
        Log.i("OpenCVCardIsolator", "  isReasonableSize=$isReasonableSize, isPortraitish=$isPortraitish")

        if (!isPortraitish) {
            Log.i("OpenCVCardIsolator", "Card detection rejected: Invalid aspect ratio ${"%.2f".format(aspectRatio)}")
            return Result.failure(ScanError.InvalidAspectRatio(aspectRatio.toFloat()))
        }

        if (!isReasonableSize) {
            Log.i("OpenCVCardIsolator", "Card detection rejected: Invalid dimensions")
            return Result.failure(ScanError.CardNotFound)
        }

        return Result.success(Unit)
    }

    private fun cropWithPadding(image: Mat, rect: Rect): Mat {
        val padding = max((rect.width * 0.02).toInt(), (rect.height * 0.02).toInt())
        val paddedRect = Rect(
            max(0, rect.x - padding),
            max(0, rect.y - padding),
            (rect.width + 2 * padding).coerceAtMost(image.width() - max(0, rect.x - padding)),
            (rect.height + 2 * padding).coerceAtMost(image.height() - max(0, rect.y - padding))
        )
        Log.i("OpenCVCardIsolator", "Card detected: ${paddedRect.width}×${paddedRect.height} at (${paddedRect.x}, ${paddedRect.y})")
        return Mat(image, paddedRect).clone()
    }
}
