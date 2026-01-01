package net.yakavenka.trialsscore.data

import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

/**
 * DAO for file storage operations.
 * Manages stream lifecycle internally for safe resource management.
 */
interface FileStorageDao {
    /**
     * Writes to the given URI using the provided block.
     * Manages stream lifecycle internally.
     */
    fun writeToUri(uri: Uri, block: (OutputStream) -> Unit)

    /**
     * Reads from the given URI using the provided suspend block.
     * Manages stream lifecycle internally.
     */
    suspend fun readFromUri(uri: Uri, block: suspend (InputStream) -> Unit)
}
