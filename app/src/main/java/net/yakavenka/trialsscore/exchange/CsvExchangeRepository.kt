package net.yakavenka.trialsscore.exchange

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
import kotlin.streams.toList

class CsvExchangeRepository {
    fun export(result: List<RiderScoreAggregate>, outputStream: OutputStream) {
        val writer = CSVWriter(OutputStreamWriter(outputStream))

        writer.writeNext(generateHeader(), false)
        result
            .map(::riderScoreAsArray)
            .forEach { writer.writeNext(it, false) }

        writer.close()
    }

    fun importRiders(inputStream: InputStream): Flow<RiderScore> = flow {

//        val parser = CSVParserBuilder()
//            .withIgnoreLeadingWhiteSpace(true)
//            .build()
//        var counter = 0
        CSVReaderBuilder((InputStreamReader(inputStream)))
//            .withCSVParser(parser)
            .build()
            .forEach { line ->
                emit(RiderScore(0, line[0].trim(), line[1].trim()))
            }
//                Log.d(TAG, "Read $counter lines from $uri")
    }

    private fun generateHeader(): Array<String> {
        val sectionsHeader = range(1, SectionScore.Set.TOTAL_SECTIONS + 1)
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