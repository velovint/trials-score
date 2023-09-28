package net.yakavenka.trialsscore

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.res.stringResource
import net.yakavenka.trialsscore.data.SectionScore

@Composable
fun LapScore(sectionScores: State<SectionScore.Set?>) {
    sectionScores.value?.let {scoreSet ->
        Row {
            Text(stringResource(id = R.string.lap_score_label).format(scoreSet.getPoints(), scoreSet.getCleans()))
        }
    }
}