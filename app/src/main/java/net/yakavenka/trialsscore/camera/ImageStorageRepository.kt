package net.yakavenka.trialsscore.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class ImageStorageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
        private const val PHOTO_DIRECTORY = "score_cards"
    }

    /**
     * Captures image to internal storage.
     * Returns Uri of saved image.
     */
    suspend fun captureImage(
        imageCapture: ImageCapture,
        riderId: Int,
        loopNumber: Int
    ): Uri = suspendCancellableCoroutine { continuation ->
        val photoDir = File(context.filesDir, PHOTO_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }

        val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val photoFile = File(photoDir, "rider_${riderId}_loop_${loopNumber}_$timestamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            // Use main executor for callback
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    continuation.resume(Uri.fromFile(photoFile))
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    /**
     * Delete image at given URI
     */
    fun deleteImage(uri: Uri): Boolean {
        return try {
            val file = File(uri.path ?: return false)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}
