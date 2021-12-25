package net.yakavenka.trialsscore

import android.app.Application
import net.yakavenka.trialsscore.data.ScoreDatabase

class TrialsScoreApplication : Application() {
    val database: ScoreDatabase by lazy { ScoreDatabase.getDatabase(this) }
}