package net.yakavenka.trialsscore.exchange

import android.net.Uri
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
import java.io.FileNotFoundException


class CsvExchangeRepositoryTest {
    private val fakeFileStorage = FakeFileStorage()
    private val sut = CsvExchangeRepository(fakeFileStorage)
    private val mockUri by lazy { mock<Uri> {} }

    @Test
    fun exportToUri_fillsGapsInSectionNumbers_whenSectionsAreMissing() = runTest {
        val rider = RiderScore(1, "Test Rider", "Expert")
        val sections = listOf(
            SectionScore(riderId = 1, loopNumber = 1, sectionNumber = 2, points = 1)
        )
        val aggregate = RiderScoreAggregate(rider, sections)

        sut.exportToUri(listOf(aggregate), mockUri)

        val csvOutput = fakeFileStorage.readStringFromUri(mockUri)
        assertThat(csvOutput, containsString("Test Rider,Expert,1,0,,1"))
    }

    @Test
    fun exportToUri_includesScores_forMultipleLoops() = runTest {
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

        sut.exportToUri(aggregates, mockUri)

        val csvOutput = fakeFileStorage.readStringFromUri(mockUri)
        assertThat("Header has S1 through S4", csvOutput, containsString("S1,S2,S3,S4"))
        assertThat("Section scores", csvOutput, containsString("0,1,2,3"))
    }

    @Test
    fun exportToUri_writesValidCsv() = runTest {
        val aggregate = sampleSectionScore()

        sut.exportToUri(listOf(aggregate), mockUri)

        val writtenData = fakeFileStorage.readStringFromUri(mockUri)
        assertThat("Data was written", writtenData, notNullValue())
        assertThat("Header", writtenData, containsString("S10"))
        assertThat("Class", writtenData, containsString("Novice"))
        assertThat("Name", writtenData, containsString("Test1"))
        assertThat("Section scores", writtenData, containsString("-1,-1"))
        assertThat("Points", writtenData, containsString("5"))
        assertThat("Cleans", writtenData, containsString("2"))
    }

    @Test(expected = FileNotFoundException::class)
    fun importRidersFromUri_handlesFileNotFoundException() = runTest {
        sut.importRidersFromUri(mockUri)
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

    @Test
    fun importRidersIgnoresInvalidLines() = runTest {
        fakeFileStorage.writeStringToUri(mockUri, "\nRider 1,Novice")

        val riders = sut.importRidersFromUri(mockUri)
        val rider = riders.first()

        assertThat(rider.name, equalTo("Rider 1"))
        assertThat(rider.riderClass, equalTo("Novice"))
    }

    @Test
    fun importRidersMapsFields() = runTest {
        fakeFileStorage.writeStringToUri(mockUri, "Rider 1,Novice\nRider 2,Advanced\n")

        val riders = sut.importRidersFromUri(mockUri)
        val rider = riders.first()

        assertThat(rider.name, equalTo("Rider 1"))
        assertThat(rider.riderClass, equalTo("Novice"))
    }

    @Test
    fun importRidersReadsFullInput() = runTest {
        fakeFileStorage.writeStringToUri(mockUri, "Rider 1,Novice\nRider 2,Advanced\n")

        val riders = sut.importRidersFromUri(mockUri)

        assertThat(riders, hasSize(2))
    }

    @Test
    fun importRidersRemovesWhitespaces() = runTest {
        fakeFileStorage.writeStringToUri(mockUri, "Rider 1 , Novice \n")

        val riders = sut.importRidersFromUri(mockUri)
        val rider = riders.first()

        assertThat(rider.name, equalTo("Rider 1"))
        assertThat(rider.riderClass, equalTo("Novice"))
    }

    @Test
    fun importRidersWithQuotes() = runTest {
        fakeFileStorage.writeStringToUri(mockUri, "\"Rider, 1\",Novice\n")

        val riders = sut.importRidersFromUri(mockUri)
        val rider = riders.first()

        assertThat(rider.name, equalTo("Rider, 1"))
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
}
