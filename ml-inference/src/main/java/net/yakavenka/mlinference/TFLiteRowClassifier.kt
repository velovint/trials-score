package net.yakavenka.mlinference

import net.yakavenka.cardscanner.RowClassifier
import net.yakavenka.cardscanner.RowImage

/**
 * TFLite-backed implementation of RowClassifier.
 * Returns predicted score for a single preprocessed row image.
 *
 * Score map: [0→0, 1→1, 2→2, 3→3, 4→5] (Trials scoring uses 0,1,2,3,5; no score of 4)
 *
 * Real TFLite inference will be wired in Slice 4.3.
 */
class TFLiteRowClassifier : RowClassifier {
    override fun classify(row: RowImage): Int = 0
}
