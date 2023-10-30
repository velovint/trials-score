package net.yakavenka.trialsscore.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }

        }
        .map { preferences ->
            Log.d("UserPreferencesRepository", "Fetching UserPreferencesRepository.userPreferencesFlow")
            val riderClasses: Set<String> =
                (preferences[PreferencesKeys.RIDER_CLASSES] ?: DEFAULT_RIDER_CLASSES_STRING)
                    .split(",")
                    .map { it.trim() }
                    .toSet()
            UserPreferences(
                numSections = preferences[PreferencesKeys.NUM_SECTIONS] ?: DEFAULT_NUM_SECTIONS,
                numLoops = preferences[PreferencesKeys.NUM_LOOPS] ?: DEFAULT_NUM_LOOPS,
                riderClasses = riderClasses
            )
        }

    suspend fun updateNumSections(numSections: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NUM_SECTIONS] = numSections
        }
    }

    suspend fun updateNumLoops(numLoops: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NUM_LOOPS] = numLoops
        }
    }

    suspend fun updateClasses(riderClasses: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RIDER_CLASSES] = riderClasses.joinToString(", ")
        }
    }

    companion object {
        const val DEFAULT_NUM_SECTIONS = 10
        const val DEFAULT_NUM_LOOPS = 3
        const val DEFAULT_RIDER_CLASSES_STRING = "Champ, Expert, Advanced, Intermediate, Novice, Vintage A, Vintage B, Exhibition"
    }
}

data class UserPreferences(val numSections: Int, val numLoops: Int, val riderClasses: Set<String>)

const val USER_PREFERENCES_NAME = "user_preferences"

private object PreferencesKeys {
    val NUM_SECTIONS = intPreferencesKey("num_sections")
    val NUM_LOOPS = intPreferencesKey("num_loops")
    val RIDER_CLASSES = stringPreferencesKey("rider_classes")
}