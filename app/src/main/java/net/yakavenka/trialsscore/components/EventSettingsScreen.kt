package net.yakavenka.trialsscore.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.yakavenka.trialsscore.ui.theme.AppTheme

/**
 * This part is incomplete and isn't wired into actual application yet
 *
 * Instead I can include existing EventSettingsFragment using the following approach
 * @see <a href="https://developer.android.com/jetpack/compose/migrate/interoperability-apis/views-in-compose#fragments-in-compose">Include Fragments in Compose</a>
 */
private const val TAG = "EventSettingsScreen"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSettingsScreen(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit event settings") },
                navigationIcon = {
                    IconButton(
                        onClick = { /* "Open nav drawer" */ }
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Localized description")
                    }
                }
            )

        },
        content = { innerPadding ->
            Column(
                modifier = modifier
                    .padding(innerPadding)
            ) {
                if (showDialog) {
                    MinimalDialog(onDismissRequest = { showDialog = false })
                }
                val prefsModifier = Modifier
                    .padding(start = 56.dp, bottom = 8.dp, top = 8.dp)
                    .fillMaxWidth()
                userPreferences.forEach { pref ->
                    TextPreference(
                        label = pref.label,
                        value = pref.value,
                        modifier = prefsModifier,
                        onSelect = { showDialog = true }
                    )
                }
            }
        }
    )

}

@Composable
fun TextPreference(label: String, value: String, modifier: Modifier = Modifier, onSelect: () -> Unit = {}) {
    Column(modifier = modifier.clickable(onClick = onSelect)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MinimalDialog(onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "This is a minimal dialog",
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(widthDp = 400, heightDp = 600)
@Composable
fun EventSettingsScreenPreview() {
    AppTheme {
        EventSettingsScreen()
    }
}

data class UserPreferenceEntry(
    val label: String,
    val value: String,
    val onClick: () -> Unit = {}
)

val userPreferences = listOf(
    UserPreferenceEntry("Number of sections", "10"),
    UserPreferenceEntry("Number of loops", "3"),
    UserPreferenceEntry("Event classes", "Expert, Advanced, Intermediate, Novice, Sportsman")
)