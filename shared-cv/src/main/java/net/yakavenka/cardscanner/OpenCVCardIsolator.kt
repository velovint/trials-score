package net.yakavenka.cardscanner

import org.opencv.core.Mat

class OpenCVCardIsolator : CardIsolator {
    override fun isolate(image: Mat): Result<Mat> = Result.success(image)
}
