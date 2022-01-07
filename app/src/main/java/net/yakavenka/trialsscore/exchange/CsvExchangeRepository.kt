package net.yakavenka.trialsscore.exchange

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

class CsvExchangeRepository {
    fun export(result: List<Any>, outputStream: OutputStream) {
        outputStream.write("Test result".toByteArray())
    }
}