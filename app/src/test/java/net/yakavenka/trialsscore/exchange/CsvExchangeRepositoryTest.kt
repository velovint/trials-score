package net.yakavenka.trialsscore.exchange

import android.net.Uri
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.yakavenka.trialsscore.data.FakeFileStorage
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.SectionScore
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream


class CsvExchangeRepositoryTest {
    private val fakeFileStorage = FakeFileStorage()
    private val sut = CsvExchangeRepository(fakeFileStorage)
    private val outputStream = ByteArrayOutputStream()
    private val mockUri by lazy { mock<Uri> {} }

    @Test
    fun export() {
        val aggregate = sampleSectionScore()

        sut.export(listOf(aggregate), outputStream)

        val actual = outputStream.toString()
        assertThat("Header", actual, containsString("S10"))
        assertThat("Class", actual, containsString("Novice"))
        assertThat("Name", actual, containsString("Test1"))
        assertThat("Section scores", actual, containsString("-1,-1"))
        assertThat("Points", actual, containsString("5"))
        assertThat("Cleans", actual, containsString("2"))
    }

    @Test
    fun export_fillsGapsInSectionNumbers_whenSectionsAreMissing() {
        val rider = RiderScore(1, "Test Rider", "Expert")
        val sections = listOf(
            SectionScore(riderId = 1, loopNumber = 1, sectionNumber = 2, points = 1)
        )
        val aggregate = RiderScoreAggregate(rider, sections)

        sut.export(listOf(aggregate), outputStream)

        val csvOutput = outputStream.toString()
        assertThat(csvOutput, containsString("Test Rider,Expert,1,0,,1"))
    }

    @Test
    fun export_includesScores_forMultipleLoops() {
        // given a score for 2 loops and 2 sections
        val rider = RiderScore(1, "Rider 1", "Expert")
        val scores = listOf(
            SectionScore(riderId = 1, loopNumber = 1, sectionNumber = 1, points = 0),
            SectionScore(riderId = 1, loopNumber = 1, sectionNumber = 2, points = 1),
            SectionScore(riderId = 1, loopNumber = 2, sectionNumber = 1, points = 2),
            SectionScore(riderId = 1, loopNumber = 2, sectionNumber = 2, points = 3)
        )
        val aggregates = listOf(
            RiderScoreAggregate(rider, scores),
        )

        sut.export(aggregates, outputStream)

        val csvOutput = outputStream.toString()
        assertThat("Header has S1 through S4", csvOutput, containsString("S1,S2,S3,S4"))
        assertThat("Section scores", csvOutput, containsString("0,1,2,3"))
    }

    @Test
    fun importRidersReadsFullInput() = runBlocking{
        val riders = sut.importRiders(sampleImportStream())

        assertThat(riders, hasSize(2))
    }

    @Test
    fun importRidersMapsFields() = runBlocking {
        val riders = sut.importRiders(sampleImportStream())
        val rider = riders.first()

        assertThat(rider.name, equalTo("Rider 1"))
        assertThat(rider.riderClass, equalTo("Novice"))
    }

    @Test
    fun importRidersRemovesWhitespaces() = runBlocking {
        val input = ByteArrayInputStream(("Rider 1 , Novice \n").toByteArray())

        val riders = sut.importRiders(input)
        val rider = riders.first()

        assertThat(rider.name, equalTo("Rider 1"))
        assertThat(rider.riderClass, equalTo("Novice"))
    }

    @Test
    fun importRidersWithQuotes() = runBlocking {
        val input = ByteArrayInputStream(("\"Rider, 1\",Novice\n").toByteArray())

        val riders = sut.importRiders(input)
        val rider = riders.first()

        assertThat(rider.name, equalTo("Rider, 1"))
    }

    @Test
    fun importRidersIgnoresInvalidLines() = runBlocking {
        val input = ByteArrayInputStream(("\nRider 1,Novice").toByteArray())

        val riders = sut.importRiders(input)
        val rider = riders.first()

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
        val sections = SectionScore.Set.createForRider(1, 30, 1).sectionScores.toMutableList()
        sections[0] = sections[0].copy(points = 0)
        sections[1] = sections[1].copy(points = 0)
        sections[2] = sections[2].copy(points = 5)
        return RiderScoreAggregate(
            RiderScore(1, "Test1", "Novice"),
            sections)
    }

    // Tests for Uri-based methods

    @Test
    fun exportToUri_writesValidCsv() = runTest {
        val aggregate = sampleSectionScore()

        sut.exportToUri(listOf(aggregate), mockUri)

        val writtenData = fakeFileStorage.readStringFromUri(mockUri)
        assertThat("Data was written", writtenData, notNullValue())
        assertThat("Header", writtenData, containsString("S10"))
        assertThat("Class", writtenData, containsString("Novice"))
        assertThat("Name", writtenData, containsString("Test1"))
    }

    @Test
    fun importRidersFromUri_readsValidCsv() = runTest {
        val csvData = "Rider 1,Novice\nRider 2,Advanced\n"
        fakeFileStorage.writeStringToUri(mockUri, csvData)

        val riders = sut.importRidersFromUri(mockUri)

        assertThat(riders, hasSize(2))
        assertThat(riders[0].name, equalTo("Rider 1"))
        assertThat(riders[0].riderClass, equalTo("Novice"))
        assertThat(riders[1].name, equalTo("Rider 2"))
        assertThat(riders[1].riderClass, equalTo("Advanced"))
    }

    @Test(expected = FileNotFoundException::class)
    fun importRidersFromUri_handlesFileNotFoundException() = runTest {
        sut.importRidersFromUri(mockUri)
    }
}
