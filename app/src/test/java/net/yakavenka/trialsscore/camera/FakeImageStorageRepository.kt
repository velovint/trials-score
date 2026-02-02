package net.yakavenka.trialsscore.camera

import android.net.Uri
import androidx.camera.core.ImageCapture

/**
 * Fake implementation of ImageStorageRepository for testing.
 *
 * Allows tests to simulate image capture success and failure scenarios
 * without actually interacting with the file system.
 */
class FakeImageStorageRepository(
    var shouldThrow: Boolean = false
) {
    /**
     * List of captured images: (imageCapture, riderId, loopNumber)
     */
    val capturedImages = mutableListOf<Triple<ImageCapture, Int, Int>>()

    /**
     * Simulates capturing an image.
     * If shouldThrow is true, throws an exception.
     * Otherwise, records the capture and returns a mock URI.
     */
    suspend fun captureImage(
        imageCapture: ImageCapture,
        riderId: Int,
        loopNumber: Int
    ): Uri {
        if (shouldThrow) {
            throw Exception("Simulated capture failure")
        }
        capturedImages.add(Triple(imageCapture, riderId, loopNumber))
        return Uri.parse("file://test_${riderId}_${loopNumber}.jpg")
    }

    /**
     * No-op delete for testing.
     */
    fun deleteImage(uri: Uri): Boolean = true
}
