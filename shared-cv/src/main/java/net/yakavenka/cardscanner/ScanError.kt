package net.yakavenka.cardscanner

sealed class ScanError : Exception() {
    object CardNotFound : ScanError()
    data class InvalidAspectRatio(val ratio: Float) : ScanError()
    data class InsufficientCells(val found: Int) : ScanError()
    data class InsufficientRows(val found: Int) : ScanError()
}
