package net.yakavenka.trialsscore.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SectionScoreTest {

    companion object {
        private const val TEST_RIDER_ID = 1
    }

    @Test
    fun blankScoreSet() {
        val sut = SectionScore.Set.createForRider(TEST_RIDER_ID, 30, 1)

        assertEquals("Points", 0, sut.getPoints())
        assertEquals("Cleans", 0, sut.getCleans())
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
        assertEquals("Size", numSections, result.sectionScores.size)
        assertEquals("First section number", 1, result.sectionScores[0].sectionNumber)
        assertEquals("Last section number", numSections, result.sectionScores[numSections - 1].sectionNumber)
        assertEquals("Loop number", loopNumber, result.getLoopNumber())
        // All should be unscored
        result.sectionScores.forEach { score ->
            assertEquals("Points for section ${score.sectionNumber}", -1, score.points)
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
        assertEquals("Size", numSections, result.sectionScores.size)

        // Missing sections should be blank
        assertEquals("Section 1 points", -1, result.sectionScores[0].points)
        assertEquals("Section 3 points", -1, result.sectionScores[2].points)

        // Existing score should be preserved
        assertEquals("Section 2 points", 3, result.sectionScores[1].points)

        // Verify correct metadata
        assertEquals("Loop number", loopNumber, result.getLoopNumber())
        assertEquals("Rider ID", TEST_RIDER_ID, result.sectionScores[0].riderId)
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
        assertEquals("Size", numSections, result.sectionScores.size)
        result.sectionScores.forEachIndexed { index, score ->
            assertEquals("Section ${index + 1} points", index + 1, score.points)
        }
    }

}