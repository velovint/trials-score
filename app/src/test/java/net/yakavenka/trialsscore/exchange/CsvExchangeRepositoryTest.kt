package net.yakavenka.trialsscore.exchange

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.SectionScore
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream


class CsvExchangeRepositoryTest {
    private val sut = CsvExchangeRepository()

    @Test
    fun export() {
        val aggregate = sampleSectionScore()
        val output = ByteArrayOutputStream()

        sut.export(listOf(aggregate), output)

        val actual = output.toString()
        assertThat("Header", actual, containsString("S10"))
        assertThat("Class", actual, containsString("Novice"))
        assertThat("Name", actual, containsString("Test1"))
        assertThat("Section scores", actual, containsString("-1,-1"))
        assertThat("Points", actual, containsString("5"))
        assertThat("Cleans", actual, containsString("2"))
    }

    @Test
    fun importRidersReadsFullInput() = runBlocking{
        val riders = sut.importRiders(sampleImportStream()).toList()

        assertThat(riders, hasSize(2))
    }

    @Test
    fun importRidersMapsFields() = runBlocking {
        val rider = sut.importRiders(sampleImportStream()).first()

        assertThat(rider.name, equalTo("Rider 1"))
        assertThat(rider.riderClass, equalTo("Novice"))
    }

    @Test
    fun importRidersRemovesWhitespaces() = runBlocking {
        val input = ByteArrayInputStream(("Rider 1 , Novice \n").toByteArray())

        val rider = sut.importRiders(input).first()

        assertThat(rider.name, equalTo("Rider 1"))
        assertThat(rider.riderClass, equalTo("Novice"))
    }

    @Test
    fun importRidersWithQuotes() = runBlocking {
        val input = ByteArrayInputStream(("\"Rider, 1\",Novice\n").toByteArray())

        val rider = sut.importRiders(input).first()

        assertThat(rider.name, equalTo("Rider, 1"))
    }

    @Test
    fun importRidersIgnoresInvalidLines() = runBlocking {
        val input = ByteArrayInputStream(("\nRider 1,Novice").toByteArray())

        val rider = sut.importRiders(input).first()

        assertThat(rider.name, equalTo("Rider 1"))
        assertThat(rider.riderClass, equalTo("Novice"))
    }

    private fun sampleImportStream(): InputStream {
        return ByteArrayInputStream(
            ("Rider 1,Novice\n" +
                    "Rider 2,Advanced\n")
                .toByteArray()
        )
    }

    private fun sampleSectionScore(): RiderScoreAggregate {
        val sections = SectionScore.Set.createForRider(1, 30).sectionScores.toMutableList()
        sections[0] = sections[0].copy(points = 0)
        sections[1] = sections[1].copy(points = 0)
        sections[2] = sections[2].copy(points = 5)
        return RiderScoreAggregate(
            RiderScore(1, "Test1", "Novice"),
            sections)
    }
}
