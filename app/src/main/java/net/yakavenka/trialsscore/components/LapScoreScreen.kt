package net.yakavenka.trialsscore.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.viewmodel.ScoreCardViewModel

@Composable
fun LapScoreScreen(scoreCardViewModel: ScoreCardViewModel) {
    val sectionScores = scoreCardViewModel.sectionScores.observeAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        sectionScores.value?.let { scoreSet ->
            LapScoreCard(
                scoreSet = scoreSet,
                modifier = Modifier.weight(1f),
                onUpdate = { sectionScore -> scoreCardViewModel.updateSectionScore(sectionScore) })
            LapScoreTotal(sectionScores = scoreSet)
        }
    }
}

@Composable
fun LapScoreCard(scoreSet: SectionScore.Set, modifier: Modifier = Modifier, onUpdate: (SectionScore) -> Unit = {}) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = scoreSet.sectionScores) { sectionScore ->
            ScoreEntryItem(
                sectionScore = sectionScore,
                modifier = Modifier.padding(8.dp),
                onUpdate = { newScore -> onUpdate(sectionScore.copy(points = newScore))}
            )
        }
    }
}

@Composable
fun ScoreEntryItem(
    sectionScore: SectionScore,
    modifier: Modifier = Modifier,
    onUpdate: (Int) -> Unit = {}
) {
    Row(modifier = modifier) {
        Text(text = sectionScore.sectionNumber.toString())
        RadioButton(selected = sectionScore.points == 0, onClick = { onUpdate(0) })
        RadioButton(selected = sectionScore.points == 1, onClick = { onUpdate(1) })
        RadioButton(selected = sectionScore.points == 2, onClick = { onUpdate(2) })
        RadioButton(selected = sectionScore.points == 3, onClick = { onUpdate(3) })
        RadioButton(selected = sectionScore.points == 5, onClick = { onUpdate(5) })
    }
}


@Preview(heightDp = 200)
@Composable
fun LazyListBottomBar() {
    Column(Modifier.fillMaxSize()) {

        LazyColumn(Modifier.weight(1f)) {
            items(50) { i ->
                Text(
                    "Row $i",
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
        Button(onClick = { println("hi") }) {
            Text("Hello")
        }
    }
}
