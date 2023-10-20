package net.yakavenka.trialsscore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.yakavenka.trialsscore.components.TrialsScoreApplicationComponent
import net.yakavenka.trialsscore.ui.theme.AppTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                TrialsScoreApplicationComponent()
            }
        }
    }
}