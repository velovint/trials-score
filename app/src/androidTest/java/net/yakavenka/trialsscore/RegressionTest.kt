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
import androidx.compose.ui.test.performTextInput
import com.github.javafaker.Faker
import org.junit.Rule
import org.junit.Test

class RegressionTest {
    @get:Rule
    val compose = createAndroidComposeRule(MainActivity::class.java)
    private val faker = Faker()
    private val numSections = 10

    @Test
    fun screenLoads() {
        val riderName = faker.name().fullName()
        addRider(riderName, "Advanced")

        val scores = List(numSections) { setOf(0, 1, 2, 3, 5).random()}
        repeat(3) {idx ->
            openScoreEntry(riderName, idx + 1)
            enterScores(scores.shuffled())
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Trials Score").assertIsDisplayed()

//        Thread.sleep(5000)
    }

    private fun addRider(riderName: String, riderClass: String) {
        compose.onNodeWithContentDescription("Add rider").performClick()
//        compose.onRoot().printToLog("currentLabelExists")
        val riderNameLabel = compose.activity.getString(R.string.rider_name_req)
        compose.onNodeWithText(riderNameLabel)
            .assertIsDisplayed()
            .performTextInput(riderName)
        val riderClassLabel = compose.activity.getString(R.string.rider_class_req)
        compose.onNodeWithText(riderClassLabel).assertIsDisplayed().performClick()
        compose.onNodeWithText(riderClass).assertIsDisplayed().performClick()
        val saveLabel = compose.activity.getString(R.string.save_action)
        compose.onNodeWithText(saveLabel).assertIsDisplayed().performClick()

        compose.onNodeWithText("Trials Score").assertIsDisplayed()
        compose.onNodeWithText(riderName).assertIsDisplayed()
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