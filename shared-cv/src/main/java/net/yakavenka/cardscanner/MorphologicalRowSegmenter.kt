package net.yakavenka.cardscanner

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class MorphologicalRowSegmenter(
    val stripHeader: Boolean = true,
    private val debugObserver: ScanDebugObserver = ScanDebugObserver.NO_OP
) : RowSegmenter {
    override fun segment(card: Mat): Result<List<RowRegion>> {
        // Phase 2b: Cell Detection
        // Step 1: Adaptive threshold
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            card, binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,  // blockSize
            2.0  // C
        )

        // Step 2: Enhance grid lines with morphological OPEN
        val vertical = Mat()
        val horizontal = Mat()
        val combined = Mat()

        val vKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(1.0, 20.0))
        val hKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(20.0, 1.0))
        val vGapKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(1.0, 80.0))
        val hGapKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(80.0, 1.0))

        Imgproc.morphologyEx(binary, vertical, Imgproc.MORPH_OPEN, vKernel)
        Imgproc.morphologyEx(vertical, vertical, Imgproc.MORPH_CLOSE, vGapKernel)
        Imgproc.morphologyEx(binary, horizontal, Imgproc.MORPH_OPEN, hKernel)
        Imgproc.morphologyEx(horizontal, horizontal, Imgproc.MORPH_CLOSE, hGapKernel)
        Core.bitwise_or(vertical, horizontal, combined)

        // Step 3: MORPH_CLOSE with 3×3 kernel to bridge broken segments
        val closed = Mat()
        val closeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(combined, closed, Imgproc.MORPH_CLOSE, closeKernel)

        // Step 4: Detect cells
        val inverted = Mat()
        Core.bitwise_not(closed, inverted)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(inverted, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Step 5: Filter contours by area (median ± ratio) and aspect ratio
        val areas = contours.map { Imgproc.contourArea(it) }.sorted()
        if (areas.isEmpty()) {
            cleanupMatResources(binary, vertical, horizontal, combined, closed, inverted,
                                vKernel, hKernel, vGapKernel, hGapKernel, closeKernel, contours)
            return Result.failure(ScanError.InsufficientCells(0))
        }

        val medianArea = areas[areas.size / 2]
        Log.i("MorphologicalRowSegmenter", "Found ${contours.size} contours, median area: $medianArea")
        val validCells = contours
            .map { Imgproc.boundingRect(it) }
            .filter { rect ->
                val area = rect.width.toDouble() * rect.height
                val aspectRatio = rect.width.toDouble() / rect.height
                area >= medianArea * 0.4 && area <= medianArea * 2.5 &&
                aspectRatio >= 0.6 && aspectRatio <= 1.8
            }
        if (validCells.isEmpty()) {
            cleanupMatResources(binary, vertical, horizontal, combined, closed, inverted,
                                vKernel, hKernel, vGapKernel, hGapKernel, closeKernel, contours)
            return Result.failure(ScanError.InsufficientCells(0))
        }
        Log.i("MorphologicalRowSegmenter", "After filtering: ${validCells.size} valid cells, Y range: ${validCells.minOf { it.y }}-${validCells.maxOf { it.y + it.height }}, card dimensions: ${card.width()}x${card.rows()}")
        val cellHeights = validCells.map { it.height }.sorted()
        val medianCellHeight = cellHeights[cellHeights.size / 2]
        Log.i("MorphologicalRowSegmenter", "Cell heights - min: ${cellHeights.first()}, max: ${cellHeights.last()}, median: $medianCellHeight")

        // Remove anomalously short cells (e.g. noise fragments above the header row)
        val normalCells = validCells.filter { it.height >= medianCellHeight * 0.6 }
        Log.i("MorphologicalRowSegmenter", "After height filter: ${normalCells.size} cells (removed ${validCells.size - normalCells.size})")

        // Step 6: Validate minimum cell count
        Log.i("MorphologicalRowSegmenter", "Detected ${normalCells.size} valid cells (need >= 45)")
        if (normalCells.size < 45) {
            cleanupMatResources(binary, vertical, horizontal, combined, closed, inverted,
                                vKernel, hKernel, vGapKernel, hGapKernel, closeKernel, contours)
            Log.e("MorphologicalRowSegmenter", "InsufficientCells: ${normalCells.size} < 45")
            return Result.failure(ScanError.InsufficientCells(normalCells.size))
        }

        // Phase 2c: Upside-Down Detection
        val cardHeight = card.rows()
        val gridMinY = normalCells.minOf { it.y }
        val gridMaxY = normalCells.maxOf { it.y + it.height }
        val gridCenterY = (gridMinY + gridMaxY) / 2.0
        val relativePosition = gridCenterY / cardHeight

        val cells: List<Rect> = if (relativePosition < 0.45) {
            // Card is upside down — invert Y coordinates
            val invertedCells = normalCells.map { rect ->
                Rect(rect.x, cardHeight - (rect.y + rect.height), rect.width, rect.height)
            }
            // Rotate the card Mat 180° in-place to match the inverted coordinates
            Core.rotate(card, card, Core.ROTATE_180)
            invertedCells
        } else {
            normalCells
        }

        // Phase 3: Y-Clustering into rows
        // Sort cells by Y coordinate (top to bottom)
        val sortedCells = cells.sortedWith(compareBy<Rect> { it.y }.thenBy { it.x })

        // Use median cell height * 0.5 as gap threshold.
        // Cells in same row: tops may differ, but gap between last cell bottom and next cell top should be small
        // Cells in different rows: gap should be larger
        val clusterGap = medianCellHeight * 0.5
        Log.i("MorphologicalRowSegmenter", "Median cell height: $medianCellHeight, cluster gap threshold: $clusterGap")

        // Cluster cells into rows
        val clusters = mutableListOf<MutableList<Rect>>()
        var currentCluster = mutableListOf(sortedCells[0])

        for (i in 1 until sortedCells.size) {
            val clusterTopY = currentCluster.first().y
            val currTop = sortedCells[i].y
            if (currTop - clusterTopY < clusterGap) {
                currentCluster.add(sortedCells[i])
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(sortedCells[i])
            }
        }
        clusters.add(currentCluster)

        val validClusters = clusters.filter { it.size >= 3 }
        Log.i("MorphologicalRowSegmenter", "Found ${clusters.size} clusters, ${validClusters.size} valid (>= 3 cells)")

        // Compute RowRegion for each cluster with small padding
        val padding = 2
        val allRowRegions = validClusters.map { cluster ->
            val top = (cluster.minOf { it.y } - padding).coerceAtLeast(0)
            val bottom = (cluster.maxOf { it.y + it.height } + padding).coerceAtMost(card.rows())
            RowRegion(top, bottom)
        }

        val scoringRows = if (stripHeader && allRowRegions.isNotEmpty()) allRowRegions.drop(1) else allRowRegions

        // Debug observer calls (before cleanup while Mats are still valid)
        debugObserver.onImage("02_enhanced_lines.png", closed)
        val cellDebug = card.clone()
        for (rect in normalCells) {
            Imgproc.rectangle(cellDebug, rect.tl(), rect.br(), Scalar(255.0), 2)
        }
        debugObserver.onImage("03_detected_cells.png", cellDebug)
        cellDebug.release()
        val rowDebug = card.clone()
        for (row in scoringRows) {
            Imgproc.rectangle(
                rowDebug,
                Point(0.0, row.top.toDouble()),
                Point(card.cols().toDouble(), row.bottom.toDouble()),
                Scalar(255.0),
                2
            )
        }
        debugObserver.onImage("04_row_bounds.png", rowDebug)
        rowDebug.release()

        // Cleanup
        cleanupMatResources(binary, vertical, horizontal, combined, closed, inverted,
                            vKernel, hKernel, vGapKernel, hGapKernel, closeKernel, contours)

        if (scoringRows.size < 10) {
            Log.e("MorphologicalRowSegmenter", "InsufficientRows after header strip: ${scoringRows.size} < 10")
            return Result.failure(ScanError.InsufficientRows(scoringRows.size))
        }

        return Result.success(scoringRows)
    }

    /**
     * Release all OpenCV Mat resources.
     * Centralizes cleanup logic to prevent resource leaks and ensure all Mats are released.
     */
    private fun cleanupMatResources(
        binary: Mat,
        vertical: Mat,
        horizontal: Mat,
        combined: Mat,
        closed: Mat,
        inverted: Mat,
        vKernel: Mat,
        hKernel: Mat,
        vGapKernel: Mat,
        hGapKernel: Mat,
        closeKernel: Mat,
        contours: List<MatOfPoint>
    ) {
        listOf(binary, vertical, horizontal, combined, closed, inverted,
               vKernel, hKernel, vGapKernel, hGapKernel, closeKernel)
            .forEach { it.release() }
        contours.forEach { it.release() }
    }
}
