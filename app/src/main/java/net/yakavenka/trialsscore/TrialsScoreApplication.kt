package net.yakavenka.trialsscore

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import net.yakavenka.trialsscore.data.ScoreDatabase

class TrialsScoreApplication : Application() {
    val database: ScoreDatabase by lazy { ScoreDatabase.getDatabase(this) }
    val preferencesDataStore by preferencesDataStore(
        name = "user_preferences"
    )
}