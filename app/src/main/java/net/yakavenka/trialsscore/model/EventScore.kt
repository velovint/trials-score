package net.yakavenka.trialsscore.model

data class EventScore(
    val riderName: String,
    val sectionScores: MutableList<Int> = MutableList(14, { 0 } )
) {
    fun getTotal(): Int {
        return sectionScores.sum()
    }
}