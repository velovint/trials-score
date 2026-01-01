package net.yakavenka.trialsscore.data

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

/**
 * DAO for file storage operations.
 * Abstracts Uri-to-stream conversion to enable testing.
 */
interface FileStorageDao {
    /**
     * Opens an output stream for writing to the given URI.
     * Caller is responsible for closing the stream.
     */
    fun openOutputStream(uri: Uri): OutputStream

    /**
     * Opens an input stream for reading from the given URI.
     * Caller is responsible for closing the stream.
     */
    fun openInputStream(uri: Uri): InputStream
}
