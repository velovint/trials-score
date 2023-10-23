package net.yakavenka.trialsscore.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class UserPreferences(val numSections: Int, val numLoops: Int, val riderClasses: Set<String>)

class UserPreferencesRepository(private val sharedPreferences: SharedPreferences) {

    val userPreferencesFlow: Flow<UserPreferences> = flowOf(fetchPreferences())

    fun fetchPreferences(): UserPreferences {
        val numSections = sharedPreferences.getString(NUM_SECTIONS_KEY, DEFAULT_NUM_SECTIONS.toString())!!.toInt()
        val riderClasses = sharedPreferences
            .getString(RIDER_CLASSES_KEY, DEFAULT_RIDER_CLASSES.joinToString(", "))!!
            .split(",")
            .map { it.trim() }
            .toSet()
        return UserPreferences(numSections, DEFAULT_NUM_LOOPS, riderClasses)
    }

    companion object {
        const val NUM_SECTIONS_KEY = "num_sections"
        const val DEFAULT_NUM_SECTIONS = 10
        const val DEFAULT_NUM_LOOPS = 3
        const val RIDER_CLASSES_KEY = "rider_classes"
        val DEFAULT_RIDER_CLASSES: Set<String> = setOf(
            "Champ",
            "Expert",
            "Advanced",
            "Intermediate",
            "Novice",
            "Vintage A",
            "Vintage B",
            "Exhibition")
    }
}