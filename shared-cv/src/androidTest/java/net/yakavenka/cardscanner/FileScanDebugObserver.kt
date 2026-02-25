package net.yakavenka.cardscanner

import android.content.Context
import androidx.test.services.storage.TestStorage
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

class FileScanDebugObserver(
    private val testStorage: TestStorage,
    private val context: Context,
) : ScanDebugObserver {
    override fun onImage(name: String, image: Mat) {
        val tempFile = File(context.cacheDir, name)
        try {
            Imgcodecs.imwrite(tempFile.absolutePath, image)
            testStorage.openOutputFile(name).use { out ->
                tempFile.inputStream().use { it.copyTo(out) }
            }
        } finally {
            tempFile.delete()
        }
    }
}
