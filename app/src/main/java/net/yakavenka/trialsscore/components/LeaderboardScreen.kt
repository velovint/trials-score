package net.yakavenka.trialsscore.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.ui.theme.AppTheme
import net.yakavenka.trialsscore.viewmodel.EventScoreViewModel
import net.yakavenka.trialsscore.viewmodel.RiderStanding


@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    viewModel: EventScoreViewModel = viewModel(),
    onAdd: () -> Unit = {},
    onRiderSelect: (RiderStanding) -> Unit = {},
    onSettings: () -> Unit = {},
    onShowFullList: () -> Unit = {}
) {
    val scores by viewModel.allScores.observeAsState(initial = emptyMap())
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
                onSettings = onSettings,
                onShowFullList = onShowFullList
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
            scores,
            modifier = modifier.padding(innerPadding),
            onRiderSelect = onRiderSelect
        )
    }

}

@Preview(widthDp = 400, heightDp = 200)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardTopBar(
    onPurge: () -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
    onSettings: () -> Unit = {},
    onShowFullList: () -> Unit = {}
) {
    var displayConfirmation by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(title = { Text("Trials Score") },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions"
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Import riders") },
                    onClick = onImport,
                    leadingIcon = {
                        Icon(
                            painterResource(id = R.drawable.ic_import_riders),
                            contentDescription = "Import riders"
                        )
                    })
                DropdownMenuItem(
                    text = { Text("Export results") },
                    onClick = onExport,
                    leadingIcon = {
                        Icon(
                            painterResource(id = R.drawable.ic_export_results),
                            contentDescription = "Export results"
                        )
                    })
                DropdownMenuItem(
                    text = { Text("Screenshot view") },
                    onClick = onShowFullList,
                    leadingIcon = {
                        Icon(
                            painterResource(id = R.drawable.baseline_screenshot_24),
                            contentDescription = "Localized description"
                        )
                    })
                DropdownMenuItem(
                    text = { Text("Clear data") },
                    onClick = { displayConfirmation = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Screenshot friendly view"
                        )
                    })
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = onSettings,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Localized description"
                        )
                    })
            }
        })

    if (displayConfirmation) {
        AlertDialog(
            title = {
                Text(text = "Delete ALL event data?")
            },
            text = {
                Text(text = "This will delete all rider scores and rider information. This action can't be undone. Make sure you have exported results before proceeding.")
            },
            onDismissRequest = { displayConfirmation = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPurge()
                        displayConfirmation = false
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Leaderboard(
    groupedScores: Map<String, List<RiderStanding>>,
    modifier: Modifier = Modifier,
    onRiderSelect: (RiderStanding) -> Unit = {}
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedScores.forEach { (riderClass, classScores) ->
            stickyHeader { ClassHeader(riderClass) }
            items(classScores) { score ->
                RiderScoreSummaryComponent(score, onSelect = onRiderSelect)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun RiderScoreSummaryComponent(score: RiderStanding, onSelect: (RiderStanding) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable(onClick = { onSelect(score) })
    ) {
        Text(text =  score.standing, modifier = Modifier.weight(0.25f))
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
    RiderStanding(RiderScoreSummary(1, "Rider 4", "A", 1, 1, 1), 1, 1, 2),
    RiderStanding(RiderScoreSummary(2, "Rider 1", "B", 0, 0, 0), 1, 1, 2),
    RiderStanding(RiderScoreSummary(3, "Rider 2", "B", 1, 1, 1), 2, 1, 2),
    RiderStanding(RiderScoreSummary(4, "Rider 3", "B", 1, 1, 1), 3, 1, 2),
    RiderStanding(RiderScoreSummary(5, "Rider 5", "B", 1, 1, 1), 5, 1, 2),
    RiderStanding(RiderScoreSummary(6, "Rider 6", "B", 1, 1, 1), 6, 1, 2),
    RiderStanding(RiderScoreSummary(7, "Rider 7", "B", 1, 1, 1), 7, 1, 2),
    RiderStanding(RiderScoreSummary(8, "Rider 8", "B", 1, 1, 1), 8, 1, 2),
    RiderStanding(RiderScoreSummary(9, "Rider 9", "B", 1, 1, 1), 9, 1, 2),
    RiderStanding(RiderScoreSummary(10, "Rider 8", "A", 1, 1, 1), 2, 1, 2)
)
val groupedScores = scores.groupBy { it.riderClass }