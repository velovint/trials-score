package net.yakavenka.trialsscore.data

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Test

class SectionScoreTest {

    companion object {
        private const val TEST_RIDER_ID = 1
    }

    @Test
    fun createForRider_returnsBlankScores_whenCreatingNewSet() {
        val sut = SectionScore.Set.createForRider(TEST_RIDER_ID, 30, 1)

        assertThat("Points", sut.getPoints(), equalTo(0))
        assertThat("Cleans", sut.getCleans(), equalTo(0))
    }

    @Test
    fun createForLoop_emptyExisting_returnsAllBlankSections() {
        // Given: No existing scores
        val loopNumber = 1
        val numSections = 10
        val existingScores = emptyList<SectionScore>()

        // When: Creating a complete set for the loop
        val result = SectionScore.Set.createForLoop(TEST_RIDER_ID, loopNumber, numSections, existingScores)

        // Then: Should return all sections with points = -1
        assertThat("Size", result.sectionScores, hasSize(numSections))
        assertThat("First section number", result.sectionScores[0].sectionNumber, equalTo(1))
        assertThat("Last section number", result.sectionScores[numSections - 1].sectionNumber, equalTo(numSections))
        assertThat("Loop number", result.getLoopNumber(), equalTo(loopNumber))
        // All should be unscored
        result.sectionScores.forEach { score ->
            assertThat("Points for section ${score.sectionNumber}", score.points, equalTo(-1))
        }
    }

    @Test
    fun createForLoop_partialExisting_mergesWithBlanks() {
        // Given: Existing score for section 2 only (sections 1 and 3 are missing)
        val loopNumber = 1
        val numSections = 3
        val existingScores = listOf(
            SectionScore(TEST_RIDER_ID, loopNumber, 2, 3)
        )

        // When: Creating a complete set for the loop
        val result = SectionScore.Set.createForLoop(TEST_RIDER_ID, loopNumber, numSections, existingScores)

        // Then: Should return all 3 sections
        assertThat("Size", result.sectionScores, hasSize(numSections))

        // Missing sections should be blank
        assertThat("Section 1 points", result.sectionScores[0].points, equalTo(-1))
        assertThat("Section 3 points", result.sectionScores[2].points, equalTo(-1))

        // Existing score should be preserved
        assertThat("Section 2 points", result.sectionScores[1].points, equalTo(3))

        // Verify correct metadata
        assertThat("Loop number", result.getLoopNumber(), equalTo(loopNumber))
        assertThat("Rider ID", result.sectionScores[0].riderId, equalTo(TEST_RIDER_ID))
    }

    @Test
    fun createForLoop_fullExisting_returnsAllExisting() {
        // Given: All sections already scored
        val loopNumber = 1
        val numSections = 5
        val existingScores = (1..numSections).map { sectionNum ->
            SectionScore(TEST_RIDER_ID, loopNumber, sectionNum, sectionNum)
        }

        // When: Creating a complete set for the loop
        val result = SectionScore.Set.createForLoop(TEST_RIDER_ID, loopNumber, numSections, existingScores)

        // Then: Should return all existing scores unchanged
        assertThat("Size", result.sectionScores, hasSize(numSections))
        result.sectionScores.forEachIndexed { index, score ->
            assertThat("Section ${index + 1} points", score.points, equalTo(index + 1))
        }
    }

}