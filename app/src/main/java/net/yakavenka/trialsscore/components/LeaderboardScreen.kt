package net.yakavenka.trialsscore.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.ui.theme.AppTheme
import net.yakavenka.trialsscore.viewmodel.EventScoreViewModel
import net.yakavenka.trialsscore.viewmodel.RiderStanding


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    viewModel: EventScoreViewModel = viewModel(),
    onAdd: () -> Unit = {},
    onRiderSelect: (RiderStanding) -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val scores by viewModel.allScores.observeAsState(initial = emptyList())
    val context = LocalContext.current
    val importPicker = rememberLauncherForActivityResult(
        contract = viewModel.importContract,
        onResult = { uri -> viewModel.importRiders(uri!!, context.contentResolver) })
    val exportPicker = rememberLauncherForActivityResult(
        contract = viewModel.exportContract,
        onResult = { uri -> viewModel.exportReport(uri!!, context.contentResolver) })

    Scaffold(
        topBar = {
            LeaderboardTopBar(
                onPurge = viewModel::clearAll,
                onImport = { importPicker.launch("text/*") },
                onExport = { exportPicker.launch("report.csv" ) },
                onSettings = onSettings
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Localized description")
            }
        }
    ) { innerPadding ->
        Leaderboard(
            scores.groupBy { score -> score.riderClass },
            modifier = modifier.padding(innerPadding),
            onRiderSelect = onRiderSelect
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardTopBar(
    onPurge: () -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    TopAppBar(title = { Text("Trials Score") },
        actions = {
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Localized description"
                )
            }
            IconButton(onClick = onImport) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Localized description"
                )
            }
            IconButton(onClick = onPurge) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Localized description"
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Localized description"
                )
            }
        })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Leaderboard(
    groupedScores: Map<String, List<RiderStanding>>,
    modifier: Modifier = Modifier,
    onRiderSelect: (RiderStanding) -> Unit = {}
) {
    LazyColumn(modifier = modifier) {
        groupedScores.forEach { (riderClass, classScores) ->
            stickyHeader { ClassHeader(riderClass) }
            items(classScores) { score ->
                RiderScoreSummaryComponent(score, onSelect = { onRiderSelect(score) })
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun RiderScoreSummaryComponent(score: RiderStanding, onSelect: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onSelect)
    ) {
        Text(text = score.standing.toString(), modifier = Modifier.weight(0.25f))
        Text(text = score.riderName, modifier = Modifier.weight(2f))
        Text(
            text = stringResource(id = R.string.lap_score, score.points, score.numCleans),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ClassHeader(riderClass: String) {
    val style = MaterialTheme.typography.labelMedium

    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "#",
                    modifier = Modifier.weight(0.25f),
                    style = style,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = riderClass.uppercase(),
                    modifier = Modifier.weight(2f),
                    style = style,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.scores_label).uppercase(),
                    modifier = Modifier.weight(1f),
                    style = style,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Divider()
        }
    }
}

@Preview(widthDp = 400, heightDp = 200)
@Composable
fun LeaderboardScreenPreview() {
    AppTheme {
        Leaderboard(groupedScores = groupedScores)
    }
}

val scores: List<RiderStanding> = listOf(
    RiderStanding(RiderScoreSummary(1, "Rider 4", "A", 1, 1, 1), 1, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 1", "B", 1, 1, 1), 1, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 2", "B", 1, 1, 1), 2, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 3", "B", 1, 1, 1), 3, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 5", "B", 1, 1, 1), 5, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 6", "B", 1, 1, 1), 6, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 7", "B", 1, 1, 1), 7, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 8", "B", 1, 1, 1), 8, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 9", "B", 1, 1, 1), 9, 1),
    RiderStanding(RiderScoreSummary(1, "Rider 8", "A", 1, 1, 1), 2, 1)
)
val groupedScores = scores.groupBy { it.riderClass }