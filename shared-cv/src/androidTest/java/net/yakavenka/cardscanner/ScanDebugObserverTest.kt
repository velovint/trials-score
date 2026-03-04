package net.yakavenka.cardscanner

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader

@RunWith(AndroidJUnit4::class)
class ScanDebugObserverTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        OpenCVLoader.initLocal()
    }

    @Test
    fun preprocessor_callsObserver_withAllExpectedImageNames() {
        // Load a real card image from test assets
        val bitmap = context.assets.open("score-card-sideways.jpg").use {
            BitmapFactory.decodeStream(it)
        }!!

        val capturedNames = mutableListOf<String>()
        val observer = ScanDebugObserver { name, _ -> capturedNames.add(name) }

        OpenCVCardPreprocessor(debugObserver = observer).preprocess(bitmap)

        assertThat(capturedNames, hasItem("01_card_boundary.png"))
        assertThat(capturedNames, hasItem("02_enhanced_lines.png"))
        assertThat(capturedNames, hasItem("03_detected_cells.png"))
        assertThat(capturedNames, hasItem("04_row_bounds.png"))
        val rowNames = capturedNames.filter { it.startsWith("05_row_") }
        assertThat(rowNames, hasSize(15))
        assertThat(rowNames, hasItem("05_row_00.png"))
        assertThat(rowNames, hasItem("05_row_14.png"))
    }

    @Test
    fun fileScanDebugObserver_writesAllExpectedFiles() {
        // Load a real card image
        val bitmap = context.assets.open("score-card-sideways.jpg").use {
            BitmapFactory.decodeStream(it)
        }!!

        val observer = FileScanDebugObserver(TestStorage(), context)
        val result = OpenCVCardPreprocessor(debugObserver = observer).preprocess(bitmap)

        assertThat("Preprocessing should succeed with debug observer attached", result.isSuccess, `is`(true))
    }
}
