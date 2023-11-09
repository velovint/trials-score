package net.yakavenka.trialsscore.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.SectionScore

@Composable
fun LapScoreTotal(sectionScores: SectionScore.Set, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(
            R.string.lap_score_label, sectionScores.getPoints(), sectionScores.getCleans()
        )
    )
}