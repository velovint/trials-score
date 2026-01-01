package net.yakavenka.trialsscore.data

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

/**
 * Fake implementation of FileStorageDao for testing.
 * Uses in-memory byte arrays to simulate file I/O.
 */
class FakeFileStorage : FileStorageDao {
    private val writtenData = mutableMapOf<String, ByteArrayOutputStream>()
    private val dataToRead = mutableMapOf<String, ByteArray>()
    private val urisToFail = mutableSetOf<String>()

    fun simulateFileNotFound(uri: Uri) {
        urisToFail.add(uri.toString())
    }

    fun setDataToRead(uri: Uri, data: ByteArray) {
        dataToRead[uri.toString()] = data
    }

    fun getWrittenData(uri: Uri): ByteArray? {
        return writtenData[uri.toString()]?.toByteArray()
    }

    fun getWrittenDataAsString(uri: Uri): String? {
        return getWrittenData(uri)?.toString(Charsets.UTF_8)
    }

    override fun openOutputStream(uri: Uri): OutputStream {
        if (urisToFail.contains(uri.toString())) {
            throw FileNotFoundException("Simulated failure for $uri")
        }
        val outputStream = ByteArrayOutputStream()
        writtenData[uri.toString()] = outputStream
        return outputStream
    }

    override fun openInputStream(uri: Uri): InputStream {
        if (urisToFail.contains(uri.toString())) {
            throw FileNotFoundException("Simulated failure for $uri")
        }
        val data = dataToRead[uri.toString()]
            ?: throw FileNotFoundException("No data set for $uri")
        return ByteArrayInputStream(data)
    }
}
