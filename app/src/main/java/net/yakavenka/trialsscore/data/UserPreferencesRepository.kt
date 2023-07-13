package net.yakavenka.trialsscore.data

import android.content.SharedPreferences

data class UserPreferences(val numSections: Int)

class UserPreferencesRepository(private val sharedPreferences: SharedPreferences) {

    fun fetchPreferences(): UserPreferences {
        val numSections = sharedPreferences.getString(NUM_SECTIONS_KEY, "30")!!.toInt()
        return UserPreferences(numSections)
    }

    companion object {
        const val NUM_SECTIONS_KEY = "num_sections"
    }
}