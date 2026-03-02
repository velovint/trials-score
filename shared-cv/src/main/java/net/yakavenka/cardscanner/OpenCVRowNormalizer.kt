package net.yakavenka.cardscanner

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extracts, resizes, and normalizes individual rows from a score card image.
 * Despite the name, this is not just a normalizer — it also crops each row region
 * from the full card and resizes it to 640×66 pixels for ML inference.
 */
class OpenCVRowNormalizer(
    private val debugObserver: ScanDebugObserver = ScanDebugObserver.NO_OP
) : RowNormalizer {
    override fun normalize(card: Mat, regions: List<RowRegion>): List<RowImage> {
        return regions.mapIndexed { index, region ->
            val crop = card.submat(region.top, region.bottom, 0, card.cols())
            debugObserver.onImage("05_row_%02d.png".format(index), crop)
            val resized = Mat()
            Imgproc.resize(crop, resized, Size(640.0, 66.0))
            val float32 = Mat()
            resized.convertTo(float32, CvType.CV_32F, 1.0 / 255.0)
            resized.release()

            val floatArray = FloatArray(66 * 640)
            float32.get(0, 0, floatArray)
            float32.release()

            val buffer = ByteBuffer.allocateDirect(66 * 640 * 4).order(ByteOrder.nativeOrder())
            buffer.asFloatBuffer().put(floatArray)
            buffer.rewind()
            RowImage(buffer)
        }
    }
}
