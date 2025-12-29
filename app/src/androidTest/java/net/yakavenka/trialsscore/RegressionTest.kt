package net.yakavenka.trialsscore

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.github.javafaker.Faker
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class RegressionTest {
    @get:Rule
    val compose = createAndroidComposeRule(MainActivity::class.java)
    private val faker = Faker()
    private val numSections = 10
    private val basicClasses = setOf("Expert", "Advanced", "Intermediate")
    private val allowedScores = setOf(0, 1, 2, 3, 5)

    @Test
    fun basicScoreEntry() {
        val riderName = addRider()

        val scores = List(numSections) { allowedScores.random()}
        repeat(3) { idx ->
            openScoreEntry(riderName, idx + 1)
            enterScores(scores.shuffled())
        }

        backToLeaderboard()
    }

    @Test
    fun clearRiderScores() {
        val scores = List(numSections) { allowedScores.random()}
        val clearRiderScoresLabel = compose.activity.getString(R.string.clear_rider_scores)
        val riderName = addRider()

        openScoreEntry(riderName, 1)
        enterScores(scores.shuffled())

        compose.onNodeWithContentDescription(clearRiderScoresLabel).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.delete_confirm)).performClick()

        compose.onNode(leaderboardTitle()).assertIsDisplayed()
    }

    @Test
    fun editRider() {
        var riderName = faker.name().fullName()
        addRider(riderName, basicClasses.random())
        openScoreEntry(riderName, 1)
        riderName = faker.name().fullName()
        performEditRider(riderName, basicClasses.random())
        backToLeaderboard()

        compose.onNodeWithText(riderName).assertExists()
    }

    @Test
    fun openScoreEntryForMultipleNewRiders() {
        // Generate multiple riders
        val riders = List(10) { addRider() }

        // For each rider, open score entry page and return to leaderboard
        riders.forEach { riderName ->
            openScoreEntry(riderName, loopNumber = 1)
            backToLeaderboard()
        }
    }

    @Ignore("Run manually for verification")
    @Test
    fun simulateEvent() {
        repeat(10) { basicScoreEntry() }

        Thread.sleep(10000)
    }

    private fun leaderboardTitle(): SemanticsMatcher {
        return hasText("Trials Score")
    }

    private fun backToLeaderboard() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back_action))
            .performClick()
        compose.onNode(leaderboardTitle()).assertIsDisplayed()
    }

    private fun performEditRider(riderName: String, riderClass: String) {
        val editRiderLabel = compose.activity.getString(R.string.edit_rider_info)
        compose.onNodeWithContentDescription(editRiderLabel).performClick()
        fillEditRiderForm(riderName, riderClass)
    }

    private fun addRider(riderName: String, riderClass: String) {
        val addRiderLabel = compose.activity.getString(R.string.add_new_rider)
        compose.onNodeWithContentDescription(addRiderLabel).performClick()
//        compose.onRoot().printToLog("currentLabelExists")
        fillEditRiderForm(riderName, riderClass)

        compose.onNodeWithText("Trials Score").assertIsDisplayed()
        compose.onNodeWithText(riderName).assertIsDisplayed()
    }

    private fun addRider(): String {
        val riderName = faker.name().fullName()
        val riderClass = basicClasses.random()
        addRider(riderName, riderClass)
        return riderName
    }

    private fun enterScores(scores: List<Int>) {
        scores.forEachIndexed { idx, score ->
            compose.onNode(sectionScoreNode(idx + 1, score))
                .performClick()
//                .assertIsSelected()
        }
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

    private fun sectionScoreNode(section: Int, score: Int): SemanticsMatcher {
        return hasParent(hasContentDescription("Section $section")) and
                hasContentDescription(score.toString())
    }

}