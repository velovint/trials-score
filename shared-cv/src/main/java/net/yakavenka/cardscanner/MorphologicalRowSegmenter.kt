package net.yakavenka.cardscanner

import android.util.Log
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class MorphologicalRowSegmenter : RowSegmenter {
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
            binary.release()
            vertical.release()
            horizontal.release()
            combined.release()
            closed.release()
            inverted.release()
            vKernel.release()
            hKernel.release()
            closeKernel.release()
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
                area >= medianArea * 0.4 && area <= medianArea * 2.5 &&
                aspectRatio >= 0.6 && aspectRatio <= 1.8
            }
        if (validCells.isEmpty()) {
            binary.release()
            vertical.release()
            horizontal.release()
            combined.release()
            closed.release()
            inverted.release()
            vKernel.release()
            hKernel.release()
            vGapKernel.release()
            hGapKernel.release()
            closeKernel.release()
            contours.forEach { it.release() }
            return Result.failure(ScanError.InsufficientCells(0))
        }
        Log.i("MorphologicalRowSegmenter", "After filtering: ${validCells.size} valid cells, Y range: ${validCells.minOf { it.y }}-${validCells.maxOf { it.y + it.height }}, card dimensions: ${card.width()}x${card.rows()}")
        val cellHeights = validCells.map { it.height }.sorted()
        Log.i("MorphologicalRowSegmenter", "Cell heights - min: ${cellHeights.first()}, max: ${cellHeights.last()}, median: ${cellHeights[cellHeights.size / 2]}")

        // Step 6: Validate minimum cell count
        Log.i("MorphologicalRowSegmenter", "Detected ${validCells.size} valid cells (need >= 45)")
        if (validCells.size < 45) {
            binary.release()
            vertical.release()
            horizontal.release()
            combined.release()
            closed.release()
            inverted.release()
            vKernel.release()
            hKernel.release()
            vGapKernel.release()
            hGapKernel.release()
            closeKernel.release()
            contours.forEach { it.release() }
            Log.e("MorphologicalRowSegmenter", "InsufficientCells: ${validCells.size} < 45")
            return Result.failure(ScanError.InsufficientCells(validCells.size))
        }

        // Phase 2c: Upside-Down Detection
        val cardHeight = card.rows()
        val gridMinY = validCells.minOf { it.y }
        val gridMaxY = validCells.maxOf { it.y + it.height }
        val gridCenterY = (gridMinY + gridMaxY) / 2.0
        val relativePosition = gridCenterY / cardHeight

        val cells: List<Rect> = if (relativePosition < 0.45) {
            // Card is upside down — invert Y coordinates
            validCells.map { rect ->
                Rect(rect.x, cardHeight - (rect.y + rect.height), rect.width, rect.height)
            }
        } else {
            validCells
        }

        // Phase 3: Y-Clustering into rows
        // Sort cells by Y coordinate (top to bottom)
        val sortedCells = cells.sortedWith(compareBy<Rect> { it.y }.thenBy { it.x })

        // Estimate cell height from median
        val medianCellHeight = sortedCells.map { it.height }.sorted().let { it[it.size / 2] }
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

        // Validate: need at least 12 clusters with >= 3 cells each
        val validClusters = clusters.filter { it.size >= 3 }
        Log.i("MorphologicalRowSegmenter", "Found ${clusters.size} clusters, ${validClusters.size} valid (>= 3 cells)")
        if (validClusters.size < 12) {
            binary.release()
            vertical.release()
            horizontal.release()
            combined.release()
            closed.release()
            inverted.release()
            vKernel.release()
            hKernel.release()
            vGapKernel.release()
            hGapKernel.release()
            closeKernel.release()
            contours.forEach { it.release() }
            Log.e("MorphologicalRowSegmenter", "InsufficientRows: ${validClusters.size} < 12")
            return Result.failure(ScanError.InsufficientRows(validClusters.size))
        }

        // Compute RowRegion for each cluster with small padding
        val padding = 2
        val rowRegions = validClusters.map { cluster ->
            val top = (cluster.minOf { it.y } - padding).coerceAtLeast(0)
            val bottom = (cluster.maxOf { it.y + it.height } + padding).coerceAtMost(card.rows())
            RowRegion(top, bottom)
        }

        // Return up to 15 rows (take first 15 if more found, or all if <= 15)
        val result = rowRegions.take(15)

        // Cleanup
        binary.release()
        vertical.release()
        horizontal.release()
        combined.release()
        closed.release()
        inverted.release()
        vKernel.release()
        hKernel.release()
        vGapKernel.release()
        hGapKernel.release()
        closeKernel.release()
        contours.forEach { it.release() }

        return Result.success(result)
    }
}
