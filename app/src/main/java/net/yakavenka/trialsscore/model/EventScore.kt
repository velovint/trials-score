package net.yakavenka.trialsscore.model

import androidx.lifecycle.MutableLiveData

data class EventScore(
    val riderName: String,
    val sectionScores: MutableList<Int> = MutableList(14, { -1 } )
) {
    val lapPoints = MutableLiveData<Int>(0)
    val cleans = MutableLiveData<Int>(0)

    fun getTotalPoints(): Int {
        return sectionScores.filter { it > 0 }.sum()
    }

    fun getCleans(): Int {
        return sectionScores.filter { it == 0 }.count()
    }
}
