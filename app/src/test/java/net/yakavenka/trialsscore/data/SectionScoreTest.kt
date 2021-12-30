package net.yakavenka.trialsscore.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SectionScoreTest {
    @Test
    fun blankScoreSet() {
        val sut = SectionScore.Set.createForRider(1)

        assertEquals("Points", 0, sut.getPoints())
        assertEquals("Cleans", 0, sut.getCleans())
    }
}