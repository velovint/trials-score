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
    private val stripHeader: Boolean = true,
    private val debugObserver: ScanDebugObserver = ScanDebugObserver.NO_OP
) : RowSegmenter {
    private companion object {
        // Area filtering thresholds: cells with area deviating from median by these ratios are filtered out
        const val MIN_CELL_AREA_RATIO = 0.4
        const val MAX_CELL_AREA_RATIO = 2.5

        // Aspect ratio filtering: cells too elongated or too squat are filtered out
        const val MIN_ASPECT_RATIO = 0.6
        const val MAX_ASPECT_RATIO = 1.8

        // Height filtering: cells significantly shorter than median are considered noise fragments
        const val MIN_HEIGHT_RATIO = 0.6

        // Orientation detection: grid position in upper portion of image indicates upside-down card
        const val UPSIDE_DOWN_THRESHOLD = 0.45

        // Clustering: gap threshold for determining row boundaries (relative to median cell height)
        const val CLUSTER_GAP_RATIO = 0.5
    }
    override fun segment(card: Mat): Result<List<RowRegion>> {
        val binary = binarize(card)
        val closed = enhanceGridLines(binary)
        val contours = findCells(closed)
        val filterResult = filterCells(contours, card)
        if (filterResult.isFailure) {
            closed.release()
            @Suppress("UNCHECKED_CAST")
            return filterResult as Result<List<RowRegion>>
        }
        val normalCells = filterResult.getOrThrow()
        val medianCellHeight = normalCells.map { it.height }.sorted().let { it[it.size / 2] }
        val cells = correctOrientation(card, normalCells)
        val allRowRegions = clusterIntoRows(cells, medianCellHeight, card.rows())
        val scoringRows = if (stripHeader && allRowRegions.isNotEmpty()) allRowRegions.drop(1) else allRowRegions
        emitDebugImages(card, normalCells, scoringRows, closed)
        closed.release()

        if (scoringRows.size < 10) {
            Log.e("MorphologicalRowSegmenter", "InsufficientRows after header strip: ${scoringRows.size} < 10")
            return Result.failure(ScanError.InsufficientRows(scoringRows.size))
        }

        return Result.success(scoringRows)
    }

    private fun binarize(card: Mat): Mat {
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            card, binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,  // blockSize
            2.0  // C
        )
        return binary
    }

    private fun enhanceGridLines(binary: Mat): Mat {
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

        val closed = Mat()
        val closeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(combined, closed, Imgproc.MORPH_CLOSE, closeKernel)

        binary.release()
        vertical.release()
        horizontal.release()
        combined.release()
        vKernel.release()
        hKernel.release()
        vGapKernel.release()
        hGapKernel.release()
        closeKernel.release()

        return closed
    }

    private fun findCells(closed: Mat): List<MatOfPoint> {
        val inverted = Mat()
        Core.bitwise_not(closed, inverted)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(inverted, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        inverted.release()
        hierarchy.release()

        return contours
    }

    private fun filterCells(contours: List<MatOfPoint>, card: Mat): Result<List<Rect>> {
        val areas = contours.map { Imgproc.contourArea(it) }.sorted()
        if (areas.isEmpty()) {
            contours.forEach { it.release() }
            return Result.failure(ScanError.InsufficientCells(0))
        }

        val medianArea = areas[areas.size / 2]
        Log.i("MorphologicalRowSegmenter", "Found ${contours.size} contours, median area: $medianArea")
        val validCells = contours
            .map { Imgproc.boundingRect(it) }
            .filter { rect ->
                val area = rect.width.toDouble() * rect.height
                val aspectRatio = rect.width.toDouble() / rect.height
                area >= medianArea * MIN_CELL_AREA_RATIO && area <= medianArea * MAX_CELL_AREA_RATIO &&
                aspectRatio >= MIN_ASPECT_RATIO && aspectRatio <= MAX_ASPECT_RATIO
            }
        if (validCells.isEmpty()) {
            contours.forEach { it.release() }
            return Result.failure(ScanError.InsufficientCells(0))
        }
        Log.i("MorphologicalRowSegmenter", "After filtering: ${validCells.size} valid cells, Y range: ${validCells.minOf { it.y }}-${validCells.maxOf { it.y + it.height }}, card dimensions: ${card.width()}x${card.rows()}")
        val cellHeights = validCells.map { it.height }.sorted()
        val medianCellHeight = cellHeights[cellHeights.size / 2]
        Log.i("MorphologicalRowSegmenter", "Cell heights - min: ${cellHeights.first()}, max: ${cellHeights.last()}, median: $medianCellHeight")

        // Remove anomalously short cells (e.g. noise fragments above the header row)
        val normalCells = validCells.filter { it.height >= medianCellHeight * MIN_HEIGHT_RATIO }
        Log.i("MorphologicalRowSegmenter", "After height filter: ${normalCells.size} cells (removed ${validCells.size - normalCells.size})")

        // Validate minimum cell count
        Log.i("MorphologicalRowSegmenter", "Detected ${normalCells.size} valid cells (need >= 45)")
        if (normalCells.size < 45) {
            contours.forEach { it.release() }
            Log.e("MorphologicalRowSegmenter", "InsufficientCells: ${normalCells.size} < 45")
            return Result.failure(ScanError.InsufficientCells(normalCells.size))
        }

        contours.forEach { it.release() }
        return Result.success(normalCells)
    }

    private fun correctOrientation(card: Mat, normalCells: List<Rect>): List<Rect> {
        val cardHeight = card.rows()
        val gridMinY = normalCells.minOf { it.y }
        val gridMaxY = normalCells.maxOf { it.y + it.height }
        val gridCenterY = (gridMinY + gridMaxY) / 2.0
        val relativePosition = gridCenterY / cardHeight

        // TODO: Bug — mutates caller-owned `card` Mat in-place. The rotated Mat must be
        //  returned to the caller so it can be used for subsequent row image extraction.
        //  Fixing this requires a `RowSegmenter` interface change (e.g. return a data class
        //  that carries both the RowRegions and the (possibly rotated) card Mat).
        //  See: https://github.com/velovint/trials-score/issues/48
        return if (relativePosition < UPSIDE_DOWN_THRESHOLD) {
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
    }

    private fun clusterIntoRows(cells: List<Rect>, medianCellHeight: Int, cardRows: Int): List<RowRegion> {
        // Sort cells by Y coordinate (top to bottom)
        val sortedCells = cells.sortedWith(compareBy<Rect> { it.y }.thenBy { it.x })

        // Use median cell height * CLUSTER_GAP_RATIO as gap threshold.
        // Cells in same row: tops may differ, but gap between last cell bottom and next cell top should be small
        // Cells in different rows: gap should be larger
        val clusterGap = medianCellHeight * CLUSTER_GAP_RATIO
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
        return validClusters.map { cluster ->
            val top = (cluster.minOf { it.y } - padding).coerceAtLeast(0)
            val bottom = (cluster.maxOf { it.y + it.height } + padding).coerceAtMost(cardRows)
            RowRegion(top, bottom)
        }
    }

    private fun emitDebugImages(card: Mat, normalCells: List<Rect>, scoringRows: List<RowRegion>, closed: Mat) {
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
    }
}
