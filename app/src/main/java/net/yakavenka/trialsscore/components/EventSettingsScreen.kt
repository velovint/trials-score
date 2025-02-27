package net.yakavenka.trialsscore.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.UserPreferences
import net.yakavenka.trialsscore.ui.theme.AppTheme
import net.yakavenka.trialsscore.viewmodel.EventSettingsViewModel
import org.apache.commons.lang3.StringUtils.isNumeric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: EventSettingsViewModel = hiltViewModel(),
    navigateBack: () -> Unit = {}
) {
    val settings: UserPreferences? by viewModel.userPreferences.observeAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Edit event settings") },
                navigationIcon = {
                    IconButton(
                        onClick = navigateBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Localized description")
                    }
                }
            )

        },
        content = { innerPadding ->
            settings?.let { settings ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    val prefsModifier = Modifier
                        .padding(start = 56.dp, bottom = 8.dp, top = 8.dp)
                        .fillMaxWidth()
                    NumericPreference(
                        label = "Number of sections",
                        value = settings.numSections.toString(),
                        modifier = prefsModifier,
                        onUpdate = viewModel::updateNumSections
                    )
                    NumericPreference(
                        label = "Number of loops",
                        value = settings.numLoops.toString(),
                        modifier = prefsModifier,
                        onUpdate = viewModel::updateNumLoops
                    )
                    TextPreference(
                        label = "Event classes",
                        value = settings.riderClasses.joinToString(", "),
                        modifier = prefsModifier,
                        onUpdate = viewModel::updateRiderClasses
                    )
                }
            }
        }
    )

}

@Composable
fun NumericPreference(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onUpdate: (Int) -> Unit = {}
) {
    TextPreference(
        label = label,
        value = value,
        modifier = modifier,
        onUpdate = { if (isNumeric(it)) { onUpdate(it.toInt()) }},
        keyboardType = KeyboardType.Number
    )
}

@Composable
fun TextPreference(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onUpdate: (String) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        TextPreferenceEditForm(
            label = label,
            value = value,
            onDismissRequest = { showDialog = false },
            onUpdate = { onUpdate(it); showDialog = false },
            keyboardType = keyboardType
        )
    }
    Column(modifier = modifier.clickable(onClick = { showDialog = true })) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TextPreferenceEditForm(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDismissRequest: () -> Unit,
    onUpdate: (String) -> Unit = {}
) {
    var valueState by remember { mutableStateOf(value) }
    val promptLabel = stringResource(R.string.enter_value_prompt)

    AlertDialog(
        title = { Text(label) },
        text = {
            OutlinedTextField(
                value = valueState,
                onValueChange = { valueState = it },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.semantics { contentDescription = promptLabel }
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onUpdate(valueState) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }
        }
    )
}

@Preview(widthDp = 400, heightDp = 600)
@Composable
fun EventSettingsScreenPreview() {
    AppTheme {
        EventSettingsScreen()
    }
}

@Preview(widthDp = 400, heightDp = 600)
@Composable
fun TextPreferencePreview() {
    var value = 1

    AppTheme {
        NumericPreference(label = "Label", value = value.toString(), onUpdate = { value = it })
    }
}