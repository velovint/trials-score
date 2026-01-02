package net.yakavenka.trialsscore.data

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

/**
 * Fake implementation of FileStorageDao for testing.
 * Uses in-memory storage to simulate file I/O with unified read/write storage.
 */
class FakeFileStorage : FileStorageDao {
    private val storage = mutableMapOf<String, ByteArray>()

    /**
     * Writes to the given URI using the provided block.
     *
     * NOTE: Unlike the actual implementation, this fake APPENDS to existing content
     * instead of overwriting it. This allows tests to verify the number of write
     * invocations by examining the accumulated content.
     */
    override fun writeToUri(uri: Uri, block: (OutputStream) -> Unit) {
        val outputStream = ByteArrayOutputStream()
        block(outputStream)
        val newData = outputStream.toByteArray()

        // Append to existing content instead of overwriting
        val existingData = storage[uri.toString()] ?: ByteArray(0)
        storage[uri.toString()] = existingData + newData
    }

    override fun <T> readFromUri(uri: Uri, block: (InputStream) -> T): T {
        val data = storage[uri.toString()]
            ?: throw FileNotFoundException("No data written to $uri")
        val inputStream = ByteArrayInputStream(data)
        return block(inputStream)
    }

    fun writeStringToUri(uri: Uri, data: String) {
        writeToUri(uri) { outputStream ->
            outputStream.write(data.toByteArray(Charsets.UTF_8))
        }
    }

    fun readStringFromUri(uri: Uri): String {
        return readFromUri(uri) { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        }
    }
}
