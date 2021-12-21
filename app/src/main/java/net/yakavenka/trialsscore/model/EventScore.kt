package net.yakavenka.trialsscore.model

data class EventScore(
    val riderName: String,
    val sectionScores: MutableList<Int> = MutableList(14, { -1 } )
) {
    fun getTotalPoints(): Int {
        return sectionScores.filter { it > 0 }.sum()
    }

    fun getCleans(): Int {
        return sectionScores.filter { it == 0 }.count()
    }
}
