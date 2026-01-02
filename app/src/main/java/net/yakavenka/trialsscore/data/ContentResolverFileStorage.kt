package net.yakavenka.trialsscore.data

import android.content.ContentResolver
import android.net.Uri
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentResolverFileStorage @Inject constructor(
    private val contentResolver: ContentResolver
) : FileStorageDao {

    override fun writeToUri(uri: Uri, block: (OutputStream) -> Unit) {
        val descriptor = contentResolver.openFileDescriptor(uri, "w")
            ?: throw FileNotFoundException("Couldn't open $uri") // TODO, just log this as error
        descriptor.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                block(outputStream)
            }
        }
    }

    override suspend fun <T> readFromUri(uri: Uri, block: suspend (InputStream) -> T): T {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Can't open file $uri")
        inputStream.use { stream ->
            return block(stream)
        }
    }
}
