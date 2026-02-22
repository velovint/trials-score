package net.yakavenka.cardscanner

fun interface RowClassifier {
    // Output: one of {0, 1, 2, 3, 5}
    fun classify(row: RowImage): Int
}
