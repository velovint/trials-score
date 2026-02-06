package net.yakavenka.trialsscore.training

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test

class FilenameParserTest {

    @Test
    fun parseScores_withValidFilename_returnsCorrectScores() {
        val result = FilenameParser.parseScores("IMG_001_012305555000005.jpg")
        assertThat(result, equalTo(listOf(0, 1, 2, 3, 0, 5, 5, 5, 5, 0, 0, 0, 0, 0, 5)))
    }

    @Test
    fun parseScores_withValidFilenameAllZeros_returnsAllZeros() {
        val result = FilenameParser.parseScores("IMG_000000000000000.jpg")
        assertThat(result, equalTo(List(15) { 0 }))
    }

    @Test
    fun parseScores_withValidFilenameAllFives_returnsAllFives() {
        val result = FilenameParser.parseScores("555555555555555_test.png")
        assertThat(result, equalTo(List(15) { 5 }))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseScores_withInvalidScore4_throwsException() {
        FilenameParser.parseScores("IMG_012340000000000.jpg")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseScores_withInvalidScore6_throwsException() {
        FilenameParser.parseScores("IMG_012360000000000.jpg")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseScores_withNoDigits_throwsException() {
        FilenameParser.parseScores("no_digits_here.jpg")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseScores_withTooFewDigits_throwsException() {
        FilenameParser.parseScores("IMG_01234.jpg")
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseScores_withEmptyFilename_throwsException() {
        FilenameParser.parseScores("")
    }
}
