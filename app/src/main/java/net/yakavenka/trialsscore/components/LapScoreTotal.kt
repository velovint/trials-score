package net.yakavenka.trialsscore.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.SectionScore

@Composable
fun LapScoreTotal(sectionScores: SectionScore.Set, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            stringResource(id = R.string.lap_score_label).format(
                sectionScores.getPoints(),
                sectionScores.getCleans()
            )
        )
    }
}