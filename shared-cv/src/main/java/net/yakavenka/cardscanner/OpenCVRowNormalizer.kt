package net.yakavenka.cardscanner

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OpenCVRowNormalizer : RowNormalizer {
    override fun normalize(card: Mat, regions: List<RowRegion>): List<RowImage> {
        return regions.map { region ->
            val crop = card.submat(region.top, region.bottom, 0, card.cols())
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
