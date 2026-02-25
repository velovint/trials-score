package net.yakavenka.cardscanner

import org.opencv.core.Mat

fun interface ScanDebugObserver {
    fun onImage(name: String, image: Mat)

    companion object {
        val NO_OP: ScanDebugObserver = ScanDebugObserver { _, _ -> }
    }
}
