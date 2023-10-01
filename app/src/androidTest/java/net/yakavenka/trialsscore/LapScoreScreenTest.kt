package net.yakavenka.trialsscore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.tooling.preview.Preview
import net.yakavenka.trialsscore.components.LapScoreCard
import net.yakavenka.trialsscore.data.SectionScore
import org.junit.Rule
import org.junit.Test

class LapScoreScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lapScoreUpdatesSelectedScore() {
        rule.setContent {
            var scoreSet by remember { mutableStateOf(SectionScore.Set.createForRider(1, 3)) }
            LapScoreCard(
                scoreSet = scoreSet,
                onUpdate = { sectionScore ->
                    scoreSet = updateSectionScore(scoreSet, sectionScore)
                })
        }
        rule.onNode(
            hasParent(hasContentDescription("Section 1")) and
                    hasContentDescription("0")
        )
            .performClick()
            .assertIsSelected()
    }

    @Test
    fun lapScoreKeepsTrackOfEnteredScores() {
        rule.setContent {
            var scoreSet by remember { mutableStateOf(SectionScore.Set.createForRider(1, 3)) }
            LapScoreCard(
                scoreSet = scoreSet,
                onUpdate = { sectionScore ->
                    scoreSet = updateSectionScore(scoreSet, sectionScore)
                })
        }
        // TODO this is terribly fragile. Find a better way to select elements
        val section1score = rule.onNode(
            hasParent(hasContentDescription("Section 1")) and
                    hasContentDescription("0")
        ).performClick()
        val section2Score = rule.onNode(
            hasParent(hasContentDescription("Section 2")) and
                    hasContentDescription("1")
        ).performClick()

        section1score.assertIsSelected()
        section2Score.assertIsSelected()
        rule.onAllNodes(
            hasParent(hasContentDescription("Section 1")) and
                    hasClickAction() and
                    hasContentDescription("0").not()
        ).assertAll(isNotSelected())
    }

    private fun updateSectionScore(
        scoreSet: SectionScore.Set,
        sectionScore: SectionScore
    ): SectionScore.Set {
        return SectionScore.Set(
            scoreSet.sectionScores
                .map { origScore ->
                    if (origScore.sectionNumber == sectionScore.sectionNumber) sectionScore else origScore
                }
                .toList()
        )
    }
}

@Composable
@Preview
fun ScoreCardPreview() {
    LapScoreCard(scoreSet = SectionScore.Set.createForRider(1, 3))
}