package net.yakavenka.trialsscore

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import net.yakavenka.trialsscore.data.ScoreDatabase
import net.yakavenka.trialsscore.data.USER_PREFERENCES_NAME

class TrialsScoreApplication : Application() {
    val database: ScoreDatabase by lazy { ScoreDatabase.getDatabase(this) }
    val preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
        name = USER_PREFERENCES_NAME,
        produceMigrations = { context ->
            // Since we don't have any migrations yet, we can just return
            // an empty map.
            listOf(SharedPreferencesMigration(context, USER_PREFERENCES_NAME))
        })
}