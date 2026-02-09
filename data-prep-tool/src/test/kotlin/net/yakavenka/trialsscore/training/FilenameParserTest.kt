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

    @Test
    fun parseScores_withMissingScoreMarker_returnsListWithNines() {
        // Filename with 9s indicating missing scores at positions 7, 9, 14
        val result = FilenameParser.parseScores("IMG_0123055959000095.jpg")

        assertThat(result.size, equalTo(15))
        assertThat(result[0], equalTo(0))
        assertThat(result[1], equalTo(1))
        assertThat(result[2], equalTo(2))
        assertThat(result[3], equalTo(3))
        assertThat(result[4], equalTo(0))
        assertThat(result[5], equalTo(5))
        assertThat(result[6], equalTo(5))
        assertThat(result[7], equalTo(9))  // Missing score
        assertThat(result[8], equalTo(5))
        assertThat(result[9], equalTo(9))  // Missing score
        assertThat(result[10], equalTo(0))
        assertThat(result[11], equalTo(0))
        assertThat(result[12], equalTo(0))
        assertThat(result[13], equalTo(0))
        assertThat(result[14], equalTo(9))  // Missing score
    }

    @Test
    fun parseScores_withAllMissingScores_returnsListOfNines() {
        val result = FilenameParser.parseScores("999999999999999_incomplete.png")

        assertThat(result.size, equalTo(15))
        assertThat(result.all { it == 9 }, equalTo(true))
    }
}
