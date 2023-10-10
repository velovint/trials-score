package net.yakavenka.trialsscore.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRiderScreen(
    viewModel: EditRiderViewModel,
    modifier: Modifier = Modifier,
    onSave: () -> Unit = {viewModel.saveRider(); }) {

    val userPreference by viewModel.userPreference.observeAsState()

    Column(modifier = modifier.padding(8.dp)) {
        TextField(
            value = viewModel.riderName,
            onValueChange = viewModel::updateRiderName,
            label = { Text(stringResource(id = R.string.rider_name_req)) },
            singleLine = true,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        )

        // We want to react on tap/press on TextField to show menu
        ExposedDropdownMenuBox(
            expanded = viewModel.riderClassExpanded,
            onExpandedChange = viewModel::toggleRiderClassExpanded,
            modifier = Modifier.padding(8.dp)
        ) {
            TextField(
                // The `menuAnchor` modifier must be passed to the text field for correctness.
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                value = viewModel.riderClass,
                onValueChange = {},
                label = { Text(stringResource(id = R.string.rider_class_req)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = viewModel.riderClassExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = viewModel.riderClassExpanded,
                onDismissRequest = { viewModel.toggleRiderClassExpanded(false) },
            ) {
                userPreference?.riderClasses?.forEach { riderClass ->
                    DropdownMenuItem(
                        text = { Text(riderClass) },
                        onClick = {
                            viewModel.updateRiderClass(riderClass)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.save_action))
        }

    }
}


@Preview(widthDp = 400, heightDp = 600)
@Composable
fun EditRiderScreenPreview() {
//    val riderScoreDao = RiderScoreDaoStub()
//    val viewModel = EditRiderViewModel(riderScoreDao, UserPreferencesRepository(SharedPreferences { }))
//
//    AppTheme {
//        EditRiderScreen(viewModel)
//    }
}

class RiderScoreDaoStub : RiderScoreDao {

    override fun getAll(): Flow<List<RiderScoreAggregate>> {
        TODO("Not yet implemented")
    }

    override fun getRider(riderId: Int): Flow<RiderScore> {
        TODO("Not yet implemented")
    }

    override fun sectionScores(riderId: Int, loopNumber: Int): Flow<List<SectionScore>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateSectionScore(sectionScore: SectionScore) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllRiders() {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllScores() {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRiderScores(riderId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun insertAll(sectionScores: List<SectionScore>) {
        TODO("Not yet implemented")
    }

    override suspend fun addRider(riderScore: RiderScore) {
        TODO("Not yet implemented")
    }

    override fun fetchSummary(): Flow<List<RiderScoreSummary>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateRider(riderScore: RiderScore) {
        TODO("Not yet implemented")
    }
}