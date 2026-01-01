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

    override fun openOutputStream(uri: Uri): OutputStream {
        val descriptor = contentResolver.openFileDescriptor(uri, "w")
            ?: throw FileNotFoundException("Couldn't open $uri")
        return FileOutputStream(descriptor.fileDescriptor)
    }

    override fun openInputStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Can't open file $uri")
    }
}
