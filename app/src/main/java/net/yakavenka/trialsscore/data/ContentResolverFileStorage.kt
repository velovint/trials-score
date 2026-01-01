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
            ?: throw FileNotFoundException("Couldn't open $uri")
        descriptor.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { outputStream ->
                block(outputStream)
            }
        }
    }

    override suspend fun readFromUri(uri: Uri, block: suspend (InputStream) -> Unit) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Can't open file $uri")
        inputStream.use { stream ->
            block(stream)
        }
    }
}
