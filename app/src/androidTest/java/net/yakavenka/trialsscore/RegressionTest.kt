package net.yakavenka.trialsscore

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.github.javafaker.Faker
import org.junit.Rule
import org.junit.Test

class RegressionTest {
    @get:Rule
    val compose = createAndroidComposeRule(MainActivity::class.java)
    private val faker = Faker()
    private val numSections = 10

    @Test
    fun basicScoreEntry() {
        val riderName = faker.name().fullName()
        addRider(riderName, "Advanced")

        val scores = List(numSections) { setOf(0, 1, 2, 3, 5).random()}
        repeat(3) { idx ->
            openScoreEntry(riderName, idx + 1)
            enterScores(scores.shuffled())
        }

        backToLeaderboard()
    }

    @Test
    fun editRider() {
        var riderName = faker.name().fullName()
        addRider(riderName, "Expert")
        openScoreEntry(riderName, 1)
        riderName = faker.name().fullName()
        performEditRider(riderName, "Novice")
        backToLeaderboard()

        compose.onNodeWithText(riderName).assertExists()
    }

    private fun backToLeaderboard() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back_action))
            .performClick()
        compose.onNodeWithText("Trials Score").assertIsDisplayed()
    }

    private fun performEditRider(riderName: String, riderClass: String) {
        val editRiderLabel = compose.activity.getString(R.string.edit_rider_info)
        compose.onNodeWithContentDescription(editRiderLabel).performClick()
        fillEditRiderForm(riderName, riderClass)

    }

    private fun addRider(riderName: String, riderClass: String) {
        compose.onNodeWithContentDescription("Add rider").performClick()
//        compose.onRoot().printToLog("currentLabelExists")
        fillEditRiderForm(riderName, riderClass)

        compose.onNodeWithText("Trials Score").assertIsDisplayed()
        compose.onNodeWithText(riderName).assertIsDisplayed()
    }

    private fun fillEditRiderForm(riderName: String, riderClass: String) {
        val riderNameLabel = compose.activity.getString(R.string.rider_name_req)
        compose.onNodeWithText(riderNameLabel)
            .assertIsDisplayed()
            .performTextReplacement(riderName)
        val riderClassLabel = compose.activity.getString(R.string.rider_class_req)
        compose.onNodeWithText(riderClassLabel).assertIsDisplayed().performClick()
        compose.onNodeWithText(riderClass).assertIsDisplayed().performClick()
        val saveLabel = compose.activity.getString(R.string.save_action)
        compose.onNodeWithText(saveLabel).assertIsDisplayed().performClick()
    }

    private fun openScoreEntry(riderName: String, loopNumber: Int) {
        compose.onNodeWithText(riderName).performClick()
        compose.onNodeWithText("Loop $loopNumber")
            .assertIsDisplayed()
            .performClick()
            .assertIsSelected()
    }

    private fun enterScores(scores: List<Int>) {
        scores.forEachIndexed { idx, score ->
            compose.onNode(sectionScoreNode(idx + 1, score))
                .performClick()
                .assertIsSelected()
        }
    }

    private fun sectionScoreNode(section: Int, score: Int): SemanticsMatcher {
        return hasParent(hasContentDescription("Section $section")) and
                hasContentDescription(score.toString())
    }

}