package net.yakavenka.trialsscore.exchange

import android.net.Uri
import android.util.Log
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVWriter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.yakavenka.trialsscore.data.ContentResolverFileStorage
import net.yakavenka.trialsscore.data.FileStorageDao
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.SectionScore
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.stream.Collectors
import java.util.stream.IntStream.range
import javax.inject.Inject

private const val TAG = "CsvExchangeRepository"

class CsvExchangeRepository constructor(
    private val fileStorage: FileStorageDao,
    private val dispatcher: CoroutineDispatcher
){
    @Inject constructor(fileStorage: FileStorageDao) : this(fileStorage, Dispatchers.IO)

    fun export(result: List<RiderScoreAggregate>, outputStream: OutputStream) {
        val writer = CSVWriter(OutputStreamWriter(outputStream))

        val numSections = result
            .flatMap { it.sections }
            .maxOfOrNull { it.sectionNumber }
            ?: 10
        val numLoops = result
            .flatMap { it.sections }
            .maxOfOrNull { it.loopNumber }
            ?: 3

        writer.writeNext(generateHeader(numSections * numLoops), false)
        result
            .map { riderScoreAsArray(it, numSections, numLoops) }
            .forEach { writer.writeNext(it, false) }

        writer.close()
    }

    suspend fun importRiders(inputStream: InputStream): List<RiderScore> = withContext(dispatcher) {
        CSVReaderBuilder(InputStreamReader(inputStream))
            .build()
            .mapNotNull { line ->
                if (line.size >= 2) {
                    RiderScore(0, line[0].trim(), line[1].trim())
                } else {
                    Log.d(TAG, "CSV line has invalid format [$line]")
                    null
                }
            }
    }

    suspend fun exportToUri(result: List<RiderScoreAggregate>, uri: Uri) {
        withContext(dispatcher) {
            fileStorage.writeToUri(uri) { outputStream ->
                export(result, outputStream)
            }
        }
    }

    suspend fun importRidersFromUri(uri: Uri): List<RiderScore> = withContext(dispatcher) {
        fileStorage.readFromUri(uri) { inputStream ->
            importRiders(inputStream)
        }
    }

    private fun generateHeader(numSections: Int): Array<String> {
        val sectionsHeader = range(1, numSections + 1)
            .mapToObj {"S${it}" }
            .collect(Collectors.toList())
        return arrayOf("Name", "Class", "Points", "Cleans").plus(sectionsHeader)
    }

    private fun riderScoreAsArray(score: RiderScoreAggregate, numSections: Int, numLoops: Int): Array<String> {
        val sectionSet = SectionScore.Set(score.sections)

        // Create a map of section number to points for quick lookup
        val sectionPointsMap = score.sections.associateBy({ it.sectionNumber + (it.loopNumber - 1) * numSections}, { it.points })

        // Generate section scores with proper gaps
        val sectionScores = (1..numSections * numLoops).map { sectionNum ->
            sectionPointsMap[sectionNum]?.toString() ?: ""
        }

        return arrayOf(
            score.riderName,
            score.riderEntity.riderClass,
            sectionSet.getPoints().toString(),
            sectionSet.getCleans().toString()
        )
            .plus(sectionScores)
    }
}