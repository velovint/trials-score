package net.yakavenka.trialsscore.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EdirRiderScreen(modifier: Modifier = Modifier) {
    var classDropdownExpanded by remember { mutableStateOf(false) }
    val options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5")
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    var riderName by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        TextField(
            value = riderName,
            onValueChange = { riderName = it },
            label = { Text("Name") },
            singleLine = true
        )

        ExposedDropdownMenuBox(
            expanded = classDropdownExpanded,
            onExpandedChange = {
                classDropdownExpanded= !classDropdownExpanded
            }
        ) {
            TextField(
                readOnly = true,
                value = selectedOptionText,
                onValueChange = { },
                label = { Text("Class") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = classDropdownExpanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(
                expanded = classDropdownExpanded,
                onDismissRequest = {
                    classDropdownExpanded = false
                }
            ) {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        onClick = {
                            selectedOptionText = selectionOption
                            classDropdownExpanded = false
                        }
                    ) {
                        Text(text = selectionOption)
                    }
                }
            }
        }

        Button(onClick = { /* Do something! */ }) {
            Text("Button")
        }

    }
}

@Preview(widthDp = 200, heightDp = 600)
@Composable
fun EditRiderScreenPreview() {
    EdirRiderScreen()
}