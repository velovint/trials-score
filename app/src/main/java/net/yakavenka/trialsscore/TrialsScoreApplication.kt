package net.yakavenka.trialsscore

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import net.yakavenka.trialsscore.data.ScoreDatabase

class TrialsScoreApplication : Application() {
    val database: ScoreDatabase by lazy { ScoreDatabase.getDatabase(this) }
    // TODO: Setup similar to eventScores
    val USER_PREFERENCES_NAME = "user_preferences"

    val preferencesDataStore by preferencesDataStore(
        name = USER_PREFERENCES_NAME
    )
}