package net.yakavenka.trialsscore.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.yakavenka.trialsscore.viewmodel.EventScoreViewModel
import net.yakavenka.trialsscore.viewmodel.RiderStanding

@Composable
fun ScreenshotLeaderboardScreen(
    modifier: Modifier = Modifier,
    viewModel: EventScoreViewModel = viewModel()
) {
    val scores: Map<String, List<RiderStanding>> by viewModel.allScores.observeAsState(initial = emptyMap())

    ScreenshotLeaderboardScreen(scores, modifier)
}

@Composable
fun ScreenshotLeaderboardScreen(
    scores: Map<String, List<RiderStanding>>,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Scaffold { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scores.forEach { (riderClass, classScores) ->
                ClassHeader(riderClass)
                classScores.forEach { score ->
                    RiderScoreSummaryComponent(score)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview
@Composable
fun ScreenshotLeaderboardScreenPreview() {
    ScreenshotLeaderboardScreen(groupedScores)
}