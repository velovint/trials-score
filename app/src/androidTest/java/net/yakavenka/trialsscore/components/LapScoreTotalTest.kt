package net.yakavenka.trialsscore.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import net.yakavenka.trialsscore.data.SectionScore
import org.junit.Rule
import org.junit.Test

class LapScoreTotalTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lapScoreTotal() {
        composeTestRule.setContent {
            LapScoreTotal(SectionScore.Set.createForRider(1, 3, 1))
        }
        composeTestRule.onNodeWithText("Lap score: 0 / 0").assertExists()
    }
}