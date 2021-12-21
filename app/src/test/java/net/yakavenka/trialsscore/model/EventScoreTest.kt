package net.yakavenka.trialsscore.model

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class EventScoreTest {
    val sut = EventScore("Event Score Test")

    @Test
    fun setScore() {
        sut.sectionScores[0] = 5

        assertThat(sut.sectionScores[0], equalTo(5))
    }

    @Test
    fun setScoreMiddleElement() {
        sut.sectionScores[4] = 5

        assertThat(sut.sectionScores[4], equalTo(5))
    }

    @Test
    fun getTotalOnEmptyScores() {
        assertThat(sut.getTotalPoints(), equalTo(0))
    }

    @Test
    fun getTotalOnMultipleEntries() {
        sut.sectionScores[0] = 1
        sut.sectionScores[3] = 2

        assertThat(sut.getTotalPoints(), equalTo(3))
    }

    @Test
    fun getCleansOnEmptyScores() {
        assertThat(sut.getCleans(), equalTo(0))
    }

    @Test
    fun getCleansOnSingleCleanSection() {
        sut.sectionScores[0] = 0

        assertThat(sut.getCleans(), equalTo(1))
    }
}