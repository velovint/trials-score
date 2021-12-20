package net.yakavenka.trialsscore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.children


class MainActivity : AppCompatActivity() {
    private val sectionScores: MutableCollection<RadioGroup> = mutableListOf()
    lateinit var lapScore: TextView;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lapScore = findViewById(R.id.lap_score);
        initScoreButtons(R.id.main_window)
    }

    private fun initScoreButtons(parentId: Int) {
        findViewById<ViewGroup>(parentId)
            .children
            .filterIsInstance<RadioGroup>()
            .flatMap {
                sectionScores.add(it)
                it.children
            }
            .filterIsInstance<RadioButton>()
            .forEach {
                it.setOnClickListener { updateLapScore() }
            }
    }

    private fun updateLapScore() {
        val sum = sectionScores
            .map { it.checkedRadioButtonId }
            .filter { it != -1 }
            .map { findViewById<RadioButton>(it) }
            .map { it.tooltipText.toString().toInt() }
            .sum()
        lapScore.text = sum.toString();
    }
}