package net.yakavenka.trialsscore.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.viewmodel.ScoreCardViewModel

@Composable
fun LoopScoreEntryScreen(
    scoreCardViewModel: ScoreCardViewModel,
    onBack: () -> Unit = {},
    onLoopSelect: (Int) -> Unit = {},
    onEditRider: (RiderScore) -> Unit = {}
) {
    val sectionScores by scoreCardViewModel.sectionScores.observeAsState()
    val userPreference by scoreCardViewModel.userPreference.observeAsState()
    val selectedLoop = scoreCardViewModel.selectedLoop
    val riderInfo by scoreCardViewModel.riderInfo.observeAsState()

    Scaffold(
        topBar = {
            ScoreEntryNavigationBar(
                riderInfo,
                onBack,
                onEditRider,
                onClearScores = { scoreCardViewModel.clearScores(it.id) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LoopSelectionBar(
                currentLoop = selectedLoop,
                totalLoops = userPreference?.numLoops ?: 1,
                onUpdate = onLoopSelect
            )
            sectionScores?.let { scoreSet ->
                Column(modifier = Modifier.padding(16.dp)) {
                    LapScoreCard(
                        scoreSet = scoreSet,
                        modifier = Modifier.weight(1f),
                        onUpdate = scoreCardViewModel::updateSectionScore)
                    LapScoreTotal(sectionScores = scoreSet)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScoreEntryNavigationBar(
    riderInfo: RiderScore?,
    onBack: () -> Unit,
    onEditRider: (RiderScore) -> Unit,
    onClearScores: (RiderScore) -> Unit = {}
) {
    var  displayConfirmation by remember {mutableStateOf(false)}

    TopAppBar(
        title = { Text(riderInfo?.name ?: "") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Localized description"
                )
            }
        },
        actions = {
            IconButton(onClick = { riderInfo?.let { onEditRider(it) }}) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Localized description"
                )
            }
            IconButton(
                onClick = {displayConfirmation = true}
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Localized description"
                )
            }

        }
    )
    if (displayConfirmation) {
        AlertDialog(
            title = {
                Text(text = "Delete rider scores?")
            },
            onDismissRequest = { displayConfirmation = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        riderInfo?.let {
                            onBack()
                            onClearScores(riderInfo)
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { displayConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LoopSelectionBar(
    currentLoop: Int,
    totalLoops: Int,
    modifier: Modifier = Modifier,
    onUpdate: (Int) -> Unit = {}
) {
    if (totalLoops < 2) return
    TabRow(selectedTabIndex = currentLoop - 1, modifier = modifier) {
        repeat(totalLoops) { loopIdx ->
            Tab(
                selected = loopIdx + 1 == currentLoop,
                onClick = { onUpdate(loopIdx + 1) },
                text = { Text(text = "Loop ${loopIdx + 1}", overflow = TextOverflow.Ellipsis) })
        }
    }
}

@Composable
fun LapScoreCard(
    scoreSet: SectionScore.Set,
    modifier: Modifier = Modifier,
    onUpdate: (SectionScore) -> Unit = {}
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = scoreSet.sectionScores,
            key = { it.sectionNumber }) { sectionScore ->
            ScoreEntryItem(
                sectionScore = sectionScore,
//                modifier = Modifier.padding(8.dp).fillMaxSize(),
                onPunch = { newScore -> onUpdate(sectionScore.copy(points = newScore)) }
            )
        }
    }
}

@Composable
fun ScoreEntryItem(
    sectionScore: SectionScore,
    modifier: Modifier = Modifier,
    onPunch: (Int) -> Unit = {}
) {
    Row(modifier = modifier.semantics(properties = {
        contentDescription = "Section ${sectionScore.sectionNumber}"
    })) {
        Text(
            text = sectionScore.sectionNumber.toString(),
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        RadioButton(
            selected = sectionScore.points == 0,
            onClick = { onPunch(0) },
            modifier = Modifier.semantics { contentDescription = "0" })
        RadioButton(
            selected = sectionScore.points == 1,
            onClick = { onPunch(1) },
            modifier = Modifier.semantics { contentDescription = "1" })
        RadioButton(selected = sectionScore.points == 2, onClick = { onPunch(2) })
        RadioButton(selected = sectionScore.points == 3, onClick = { onPunch(3) })
        RadioButton(selected = sectionScore.points == 5, onClick = { onPunch(5) })
    }
}

@Composable
@Preview
fun ScoreCardPreview() {
    MaterialTheme {
        Column {
            LoopSelectionBar(currentLoop = 1, totalLoops = 3)
            LapScoreCard(scoreSet = SectionScore.Set.createForRider(1, 3, 3))
        }
    }
}