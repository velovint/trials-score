package net.yakavenka.trialsscore.exchange

import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.SectionScore
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.Test
import java.io.ByteArrayOutputStream


class CsvExchangeRepositoryTest {
    @Test
    fun basicTest() {
        val aggregate = sampleSectionScore()
        val sut = CsvExchangeRepository()
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

    private fun sampleSectionScore(): RiderScoreAggregate {
        val sections = SectionScore.Set.createForRider(1).sectionScores.toMutableList()
        sections[0] = sections[0].copy(points = 0)
        sections[1] = sections[1].copy(points = 0)
        sections[2] = sections[2].copy(points = 5)
        return RiderScoreAggregate(
            RiderScore(1, "Test1", "Novice"),
            sections)
    }
}
