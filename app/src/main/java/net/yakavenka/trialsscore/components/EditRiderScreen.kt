package net.yakavenka.trialsscore.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRiderScreen(
    viewModel: EditRiderViewModel,
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit = {}
) {

    val userPreference by viewModel.userPreference.observeAsState()

    Column(modifier = modifier.padding(8.dp)) {
        val riderEntry = viewModel.riderInfoState.entry
        val isClassExpanded = viewModel.riderInfoState.riderClassExpanded
        val coroutineScope = rememberCoroutineScope()

        TextField(
            value = riderEntry.name,
            onValueChange = { viewModel.updateUiState(riderEntry.copy(name = it)) },
            label = { Text(stringResource(id = R.string.rider_name_req)) },
            singleLine = true,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        // We want to react on tap/press on TextField to show menu
        ExposedDropdownMenuBox(
            expanded = isClassExpanded,
            onExpandedChange = viewModel::toggleRiderClassExpanded,
            modifier = Modifier.padding(8.dp)
        ) {
            TextField(
                // The `menuAnchor` modifier must be passed to the text field for correctness.
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
                readOnly = true,
                value = riderEntry.riderClass,
                onValueChange = {},
                label = { Text(stringResource(id = R.string.rider_class_req)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isClassExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = isClassExpanded,
                onDismissRequest = { viewModel.toggleRiderClassExpanded(false) },
            ) {
                userPreference?.riderClasses?.forEach { riderClass ->
                    DropdownMenuItem(
                        text = { Text(riderClass) },
                        onClick = {
                            viewModel.updateUiState(riderEntry.copy(riderClass = riderClass))
                            viewModel.toggleRiderClassExpanded(false)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.saveRider()
                }
                navigateBack()
            },
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
fun EditRiderScreenPreview() {}
