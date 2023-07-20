package net.yakavenka.trialsscore.viewmodel

import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.data.UserPreferences
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class RiderStandingTransformationTest {
    private val classes = setOf("Advanced", "Intermediate", "Novice")
    private val numSections = 5
    private val sut = RiderStandingTransformation()

    @Test
    fun fetchSummarySortsResultByClass() {
        val summary = listOf(
            RiderScoreSummary(1, "Rider1", "Novice", SectionScore.Set.TOTAL_SECTIONS, 5, 8, SectionScore.Set.TOTAL_SECTIONS),
            RiderScoreSummary(2, "Rider2", "Advanced", SectionScore.Set.TOTAL_SECTIONS, 5, 8, SectionScore.Set.TOTAL_SECTIONS),
            RiderScoreSummary(3, "Rider3", "Novice", SectionScore.Set.TOTAL_SECTIONS, 5, 8, SectionScore.Set.TOTAL_SECTIONS)
        )

        val actual = sut.invoke(summary, UserPreferences(numSections, classes)).map { it.riderClass }

        assertThat("Rider classes", actual, equalTo(listOf("Advanced", "Novice", "Novice")))
    }

    @Test
    fun fetchSummarySetsStandingsWithinAClass() {
        val summary = listOf(
            RiderScoreSummary(1, "Novice Second", "Novice", SectionScore.Set.TOTAL_SECTIONS, 5, 8, SectionScore.Set.TOTAL_SECTIONS),
            RiderScoreSummary(2, "Rider2", "Advanced", SectionScore.Set.TOTAL_SECTIONS, 5, 8, SectionScore.Set.TOTAL_SECTIONS),
            RiderScoreSummary(3, "Novice Winner", "Novice", SectionScore.Set.TOTAL_SECTIONS, 2, 8, SectionScore.Set.TOTAL_SECTIONS)
        )

        val actual = sut.invoke(summary, UserPreferences(numSections, classes)).filter { it.riderClass == "Novice" }

        assertThat("Top novice", actual[0].riderName, equalTo("Novice Winner"))
        assertThat("Top place", actual[0].standing, equalTo(1))
        assertThat("Second novice", actual[1].standing, equalTo(2))
    }

}