package net.yakavenka.trialsscore.exchange

import android.util.Log
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.SectionScore
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.stream.IntStream.range
import javax.inject.Inject
import kotlin.streams.toList

private const val TAG = "CsvExchangeRepository"

class CsvExchangeRepository @Inject constructor(){
    fun export(result: List<RiderScoreAggregate>, outputStream: OutputStream) {
        val writer = CSVWriter(OutputStreamWriter(outputStream))

        writer.writeNext(generateHeader(result), false)
        result
            .map(::riderScoreAsArray)
            .forEach { writer.writeNext(it, false) }

        writer.close()
    }

    fun importRiders(inputStream: InputStream): Flow<RiderScore> = flow {

        CSVReaderBuilder((InputStreamReader(inputStream)))
            .build()
            .forEach { line ->
                if (line.size < 2) {
                    Log.d(TAG, "CSV line has invalid format [$line]")
                } else {
                    emit(RiderScore(0, line[0].trim(), line[1].trim()))
                }
            }
    }

    private fun generateHeader(result: List<RiderScoreAggregate>): Array<String> {
        val numSections = result.map { it.sections.size }.firstOrNull() ?: 30
        val sectionsHeader = range(1, numSections + 1)
            .mapToObj {"S${it}" }
            .toList()
        return arrayOf("Name", "Class", "Points", "Cleans").plus(sectionsHeader)
    }

    private fun riderScoreAsArray(score: RiderScoreAggregate): Array<String> {
        val sectionSet = SectionScore.Set(score.sections)
        return arrayOf(
            score.riderName,
            score.riderEntity.riderClass,
            sectionSet.getPoints().toString(),
            sectionSet.getCleans().toString()
        )
            .plus(score.sections.map { it.points.toString() })
    }
}