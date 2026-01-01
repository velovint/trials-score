package net.yakavenka.trialsscore

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.github.javafaker.Faker
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.File

class RegressionTest {
    @get:Rule
    val compose = createAndroidComposeRule(MainActivity::class.java)
    private val faker = Faker()
    private val numSections = 10
    private val basicClasses = setOf("Expert", "Advanced", "Intermediate")
    private val allowedScores = setOf(0, 1, 2, 3, 5)

    @Test
    fun basicScoreEntry() {
        val riderName = addRider()

        val scores = List(numSections) { allowedScores.random()}
        repeat(3) { idx ->
            openScoreEntry(riderName, idx + 1)
            enterScores(scores.shuffled())
        }

        backToLeaderboard()
    }

    @Test
    fun clearRiderScores() {
        val scores = List(numSections) { allowedScores.random()}
        val clearRiderScoresLabel = compose.activity.getString(R.string.clear_rider_scores)
        val riderName = addRider()

        openScoreEntry(riderName, 1)
        enterScores(scores.shuffled())

        compose.onNodeWithContentDescription(clearRiderScoresLabel).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.delete_confirm)).performClick()

        compose.onNode(leaderboardTitle()).assertIsDisplayed()
    }

    @Test
    fun editRider() {
        var riderName = faker.name().fullName()
        addRider(riderName, basicClasses.random())
        openScoreEntry(riderName, 1)
        riderName = faker.name().fullName()
        performEditRider(riderName, basicClasses.random())
        backToLeaderboard()

        compose.onNodeWithText(riderName).assertExists()
    }

    @Test
    fun openScoreEntryForMultipleNewRiders() {
        // Generate multiple riders
        val riders = List(10) { addRider() }

        // For each rider, open score entry page and return to leaderboard
        riders.forEach { riderName ->
            openScoreEntry(riderName, loopNumber = 1)
            backToLeaderboard()
        }
    }

    @Ignore("Run manually for verification")
    @Test
    fun simulateEvent() {
        repeat(10) { basicScoreEntry() }

        Thread.sleep(10000)
    }

    @Test
    fun exportScoresAsCsv() {
        // Given: a rider with entered scores
        val riderName = addRider()
        val scores = List(numSections) { allowedScores.random() }
        openScoreEntry(riderName, 1)
        enterScores(scores)
        backToLeaderboard()

        // When: export results as CSV
        val moreActionsLabel = compose.activity.getString(R.string.more_actions)
        compose.onNodeWithContentDescription(moreActionsLabel).performClick()
        compose.onNodeWithText("Export results").performClick()

        // Handle system file picker using UIAutomator
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val filename = "test_export_${System.currentTimeMillis()}"

        // Wait for document picker to appear
        device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 5000)

        // Enter filename in the text field
        val filenameField = device.wait(
            Until.findObject(By.res("com.android.documentsui:id/item_root")),
            3000
        ) ?: device.findObject(By.clazz("android.widget.EditText"))
        filenameField?.text = filename

        // Click Save button
        val saveButton = device.findObject(By.text("Save"))
            ?: device.findObject(By.text("SAVE"))
        saveButton?.click()

        // Wait for file to be written
        Thread.sleep(2000)

        // Then: verify file exists and is non-empty in Downloads directory
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val file = File(downloadsDir, "$filename.csv")
        assert(file.exists()) { "Exported CSV file should exist at ${file.absolutePath}" }
        assert(file.length() > 0) { "Exported CSV file should not be empty" }

        // Cleanup
        file.delete()
    }

    private fun leaderboardTitle(): SemanticsMatcher {
        return hasText("Trials Score")
    }

    private fun backToLeaderboard() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back_action))
            .performClick()
        compose.onNode(leaderboardTitle()).assertIsDisplayed()
    }

    private fun performEditRider(riderName: String, riderClass: String) {
        val editRiderLabel = compose.activity.getString(R.string.edit_rider_info)
        compose.onNodeWithContentDescription(editRiderLabel).performClick()
        fillEditRiderForm(riderName, riderClass)
    }

    private fun addRider(riderName: String, riderClass: String) {
        val addRiderLabel = compose.activity.getString(R.string.add_new_rider)
        compose.onNodeWithContentDescription(addRiderLabel).performClick()
//        compose.onRoot().printToLog("currentLabelExists")
        fillEditRiderForm(riderName, riderClass)

        compose.onNodeWithText("Trials Score").assertIsDisplayed()
        compose.onNodeWithText(riderName).assertIsDisplayed()
    }

    private fun addRider(): String {
        val riderName = faker.name().fullName()
        val riderClass = basicClasses.random()
        addRider(riderName, riderClass)
        return riderName
    }

    private fun enterScores(scores: List<Int>) {
        scores.forEachIndexed { idx, score ->
            compose.onNode(sectionScoreNode(idx + 1, score))
                .performClick()
//                .assertIsSelected()
        }
    }

    private fun fillEditRiderForm(riderName: String, riderClass: String) {
        val riderNameLabel = compose.activity.getString(R.string.rider_name_req)
        compose.onNodeWithText(riderNameLabel)
            .assertIsDisplayed()
            .performTextReplacement(riderName)
        val riderClassLabel = compose.activity.getString(R.string.rider_class_req)
        compose.onNodeWithText(riderClassLabel).assertIsDisplayed().performClick()
        compose.onAllNodesWithText(riderClass).onLast().performClick()
        val saveLabel = compose.activity.getString(R.string.save_action)
        compose.onNodeWithText(saveLabel).assertIsDisplayed().performClick()
    }

    private fun openScoreEntry(riderName: String, loopNumber: Int) {
        compose.onNodeWithText(riderName).performClick()
        compose.onNodeWithText("Loop $loopNumber")
            .assertIsDisplayed()
            .performClick()
            .assertIsSelected()
    }

    private fun sectionScoreNode(section: Int, score: Int): SemanticsMatcher {
        return hasParent(hasContentDescription("Section $section")) and
                hasContentDescription(score.toString())
    }

}