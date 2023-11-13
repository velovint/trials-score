package net.yakavenka.trialsscore.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import net.yakavenka.trialsscore.R
import org.junit.Rule
import org.junit.Test

class PreferenceTest {
    @get:Rule
    val rule = createComposeRule()
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun numericPreferenceUpdatesTheValue() {
        var value by mutableStateOf(1)

        rule.setContent {
            NumericPreference(label = "Label", value = value.toString(), onUpdate = { value = it })
        }
        changeValue("2")

        rule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun numericPreferenceIgnoresNonNumericValue() {
        var value by mutableStateOf(1)

        rule.setContent {
            NumericPreference(label = "Label", value = value.toString(), onUpdate = { value = it })
        }
        changeValue("-2")

        rule.onNodeWithText("1").assertIsDisplayed()

        changeValue("abc")

        rule.onNodeWithText("1").assertIsDisplayed()
    }

    private fun changeValue(newValue: String) {
        rule.onNodeWithText("Label").performClick()
        rule.onNodeWithContentDescription(context.getString(R.string.enter_value_prompt))
            .performTextReplacement(newValue)
        rule.onNodeWithText("Save").performClick()
    }

}