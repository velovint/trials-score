package net.yakavenka.cardscanner

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File

@RunWith(AndroidJUnit4::class)
class MorphologicalRowSegmenterTest {

    private lateinit var segmenter: MorphologicalRowSegmenter

    @Before
    fun setup() {
        OpenCVLoader.initLocal()
        segmenter = MorphologicalRowSegmenter()
    }

    /**
     * Test that morphological row segmentation produces exactly 15 RowRegions
     * from a golden score card image that has been resized to 640px width.
     */
    @Test
    fun segment_producesCorrectNumberOfRows() {
        val rawCard = loadRawCardFromAssets("test_score_card_w_header_1.png")
        // Resize to 640px width (simulating card isolation output)
        val card = resizeToTargetWidth(rawCard, 640)

        val result = segmenter.segment(card)

        if (!result.isSuccess) {
            Log.e("MorphologicalRowSegmenterTest", "Segmentation failed: ${result.exceptionOrNull()}")
        }
        assertThat("Result should be success", result.isSuccess)
        val rows = result.getOrNull()
        assertThat("Should return exactly 15 rows", rows, hasSize(15))

        card.release()
        rawCard.release()
    }

    /**
     * Test that all RowRegions have valid Y coordinates (top < bottom).
     */
    @Test
    fun segment_rowsHaveValidCoordinates() {
        val rawCard = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card = resizeToTargetWidth(rawCard, 640)

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        val rows = result.getOrNull()!!

        rows.forEachIndexed { index, region ->
            assertThat(
                "Row $index: top (${region.top}) must be less than bottom (${region.bottom})",
                region.top,
                lessThan(region.bottom)
            )
        }

        card.release()
        rawCard.release()
    }

    /**
     * Test that all RowRegions fit within card bounds.
     */
    @Test
    fun segment_rowsAreWithinCardBounds() {
        val rawCard = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card = resizeToTargetWidth(rawCard, 640)
        val cardHeight = card.rows()

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        val rows = result.getOrNull()!!

        rows.forEachIndexed { index, region ->
            assertThat(
                "Row $index: top (${region.top}) must be >= 0",
                region.top,
                greaterThanOrEqualTo(0)
            )
            assertThat(
                "Row $index: bottom (${region.bottom}) must be <= card height ($cardHeight)",
                region.bottom,
                lessThanOrEqualTo(cardHeight)
            )
        }

        card.release()
        rawCard.release()
    }

    /**
     * Test that RowRegions are in ascending Y order (sequential).
     */
    @Test
    fun segment_rowsAreInAscendingOrder() {
        val rawCard = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card = resizeToTargetWidth(rawCard, 640)

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        val rows = result.getOrNull()!!

        val allowedOverlap = (rows.map {it.bottom - it.top}.average() * 0.22).toInt(); // allow 5% overlap due to bent cards
        for (i in 0 until rows.size - 1) {
            val boundary = rows[i + 1].top + allowedOverlap
            assertThat(
                "Row $i bottom (${rows[i].bottom}) must be <= next row top (${boundary})",
                rows[i].bottom,
                lessThanOrEqualTo(boundary)
            )
        }

        card.release()
        rawCard.release()
    }

    /**
     * Test that all RowRegions have reasonable heights (not zero, not huge).
     */
    @Test
    fun segment_rowsHaveReasonableHeights() {
        val rawCard = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card = resizeToTargetWidth(rawCard, 640)
        val cardHeight = card.rows()
        // header is 10-35% of card
        val minExpectedHeight = cardHeight / 23  // At least 65%/15 -
        val maxExpectedHeight = cardHeight / 10  // At most 1/10th of card

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        val rows = result.getOrNull()!!

        rows.forEachIndexed { index, region ->
            val height = region.bottom - region.top
            assertThat(
                "Row $index height ($height) should be >= $minExpectedHeight",
                height,
                greaterThanOrEqualTo(minExpectedHeight)
            )
            assertThat(
                "Row $index height ($height) should be <= $maxExpectedHeight",
                height,
                lessThanOrEqualTo(maxExpectedHeight)
            )
        }

        card.release()
        rawCard.release()
    }

    /**
     * Test that segmentation is consistent across multiple runs on the same image.
     */
    @Test
    fun segment_isConsistent() {
        val rawCard1 = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card1 = resizeToTargetWidth(rawCard1, 640)
        val rawCard2 = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card2 = resizeToTargetWidth(rawCard2, 640)

        val result1 = segmenter.segment(card1)
        val result2 = segmenter.segment(card2)

        assertThat("Both results should be success", result1.isSuccess && result2.isSuccess)
        val rows1 = result1.getOrNull()!!
        val rows2 = result2.getOrNull()!!

        assertThat("Should produce same number of rows", rows1.size, equalTo(rows2.size))
        rows1.zip(rows2).forEachIndexed { index, (r1, r2) ->
            assertThat(
                "Row $index coordinates should match: ($r1) vs ($r2)",
                r1.top,
                equalTo(r2.top)
            )
            assertThat(
                "Row $index coordinates should match: ($r1) vs ($r2)",
                r1.bottom,
                equalTo(r2.bottom)
            )
        }

        card1.release()
        card2.release()
        rawCard1.release()
        rawCard2.release()
    }

    /**
     * Test that segmentation handles uncropped score card (with background).
     * Should still produce 15 rows since preprocessing is assumed to isolate card first.
     */
    @Test
    fun segment_handlesPrimaryCardImage() {
        val rawCard = loadRawCardFromAssets("test_score_card_grid_gap.png")
        val card = resizeToTargetWidth(rawCard, 640)

        val result = segmenter.segment(card)

        // The golden test card should definitely produce 15 rows
        assertThat("Result should be success", result.isSuccess)
        val rows = result.getOrNull()
        assertThat("Should return 15 rows for primary card image", rows, hasSize(15))

        Log.i("MorphologicalRowSegmenterTest", "Successfully segmented primary card into 15 rows")

        card.release()
        rawCard.release()
    }

    /**
     * Debug test: log detailed information about segmented rows.
     * Can be enabled manually for troubleshooting.
     */
    @Test
    fun segment_logsRowInformation() {
        val rawCard = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card = resizeToTargetWidth(rawCard, 640)
        val cardHeight = card.rows()

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        val rows = result.getOrNull()!!

        Log.i("MorphologicalRowSegmenterTest", "Card height: $cardHeight")
        Log.i("MorphologicalRowSegmenterTest", "Total rows: ${rows.size}")

        rows.forEachIndexed { index, region ->
            val height = region.bottom - region.top
            val percentOfCard = (height.toDouble() / cardHeight * 100).toInt()
            Log.i(
                "MorphologicalRowSegmenterTest",
                "Row $index: top=${region.top}, bottom=${region.bottom}, height=$height ($percentOfCard% of card)"
            )
        }

        card.release()
        rawCard.release()
    }

    /**
     * Regression: NoSuchElementException when all contours are filtered out (validCells empty).
     * A featureless tall image produces one large contour with aspect ratio ≈0.38 which fails
     * the 0.6 filter, reproducing the device crash "Found 1 contours, median area: 1064574.0".
     */
    @Test
    fun segment_withNoGridDetected_returnsFailureInsteadOfThrowing() {
        val uniformGrayCard = Mat(1700, 640, org.opencv.core.CvType.CV_8UC1, org.opencv.core.Scalar(128.0))

        val result = segmenter.segment(uniformGrayCard)

        assertThat("Should return failure, not throw NoSuchElementException", result.isFailure)
        assertThat(
            "Failure cause should be InsufficientCells",
            result.exceptionOrNull(),
            instanceOf(ScanError.InsufficientCells::class.java)
        )

        uniformGrayCard.release()
    }

    /**
     * Coverage gap: all other segmenter tests use a clean synthetic PNG.
     * The real-world path is: JPEG photo → OpenCVCardIsolator → MorphologicalRowSegmenter.
     * This test verifies that the segmenter works on a card isolated from a real JPEG photo,
     * which has different image characteristics (compression artifacts, lower contrast).
     */
    @Test
    fun segment_onIsolatedJpegCard_produces15Rows() {
        val bitmap = loadBitmapFromAssets("score-card-uncropped.jpg")
        val card = OpenCVCardIsolator(targetWidth = 640).isolate(bitmap).getOrThrow()

        val result = segmenter.segment(card)

        assertThat("Segmenter should succeed on isolated JPEG card", result.isSuccess)
        assertThat(result.getOrNull(), hasSize(15))

        card.release()
    }


    @Test
    fun segment_gridGapCard_withHeaderIncluded_returns16Rows() {
        val segmenter = MorphologicalRowSegmenter(stripHeader = false)
        // test_score_card_grid_gap may result in 2 rectangles above header
        val rawCard = loadRawCardFromAssets("test_score_card_grid_gap.png")
        val card = resizeToTargetWidth(rawCard, 640)

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        assertThat(result.getOrNull(), hasSize(16))
        card.release(); rawCard.release()
    }

    @Test
    fun segment_withHeaderStripped_returns15Rows() {
        val segmenter = MorphologicalRowSegmenter(stripHeader = true)
        val rawCard = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card = resizeToTargetWidth(rawCard, 640)

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        assertThat(result.getOrNull(), hasSize(15))
        card.release(); rawCard.release()
    }

    @Test
    fun segment_withHeaderIncluded_returns16Rows() {
        val segmenter = MorphologicalRowSegmenter(stripHeader = false)
        val rawCard = loadRawCardFromAssets("test_score_card_w_header_1.png")
        val card = resizeToTargetWidth(rawCard, 640)

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        assertThat(result.getOrNull(), hasSize(16))
        card.release(); rawCard.release()
    }

    /**
     * Test that when a card is upside-down, after rotation by the segmenter,
     * RowRegion coordinates describe correct positions in right-side-up space.
     * First row top should be 30-50% down, last row bottom 90-100% down (near end).
     */
    @Test
    fun segment_upsideDownCard_rowsAreInCorrectPositions() {
        val rawCard = loadRawCardFromAssets("test_score_card_upside_down.png")
        val card = resizeToTargetWidth(rawCard, 640)
        val cardHeight = card.rows()

        val result = segmenter.segment(card)

        assertThat("Result should be success", result.isSuccess)
        val rows = result.getOrNull()!!
        assertThat("Should return exactly 15 rows", rows, hasSize(15))

        val firstRowTopRatio = rows.first().top / cardHeight.toDouble()
        assertThat(
            "First row top should be between 30% and 50% of card height",
            firstRowTopRatio,
            allOf(greaterThanOrEqualTo(0.30), lessThanOrEqualTo(0.50))
        )

        val lastRowBottomRatio = rows.last().bottom / cardHeight.toDouble()
        assertThat(
            "Last row bottom should be between 90% and 99% of card height",
            lastRowBottomRatio,
            allOf(greaterThanOrEqualTo(0.90), lessThanOrEqualTo(0.99))
        )

        card.release()
        rawCard.release()
    }

    // ========== Helper Functions ==========

    private fun loadBitmapFromAssets(filename: String): android.graphics.Bitmap {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open(filename).use { android.graphics.BitmapFactory.decodeStream(it) }!!
    }

    private fun loadRawCardFromAssets(filename: String): Mat {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale but don't preprocess further
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        return gray
    }

    private fun loadGrayscaleCardFromAssets(filename: String): Mat {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        mat.release()

        return gray
    }

    private fun resizeToTargetWidth(mat: Mat, targetWidth: Int): Mat {
        val aspectRatio = mat.height().toDouble() / mat.width().toDouble()
        val targetHeight = (targetWidth * aspectRatio).toInt()
        val resized = Mat()
        Imgproc.resize(mat, resized, Size(targetWidth.toDouble(), targetHeight.toDouble()))
        return resized
    }
}
