package net.yakavenka.trialsscore

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import net.yakavenka.trialsscore.data.ScoreDatabase

class TrialsScoreApplication : Application() {
    val database: ScoreDatabase by lazy { ScoreDatabase.getDatabase(this) }
    val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
}