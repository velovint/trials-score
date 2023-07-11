package net.yakavenka.trialsscore.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class UserPreferences(val numSections: Int)

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    private object PreferencesKeys {
        val NUM_SECTIONS = intPreferencesKey("num_sections")
    }

    val userPreferencesFlow = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // Get NUM_SECTIONS preference key
            val numSections = preferences[PreferencesKeys.NUM_SECTIONS] ?: 1
            UserPreferences(numSections)
        }

    suspend fun updateNumSections(numSections: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NUM_SECTIONS] = numSections
        }
    }
}