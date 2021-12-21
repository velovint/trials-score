package net.yakavenka.trialsscore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.adapter.EventScoreAdapter
import net.yakavenka.trialsscore.model.EventScore


class MainActivity : AppCompatActivity() {
    private val sectionScores: MutableCollection<RadioGroup> = mutableListOf()
    lateinit var lapScore: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.lap_score_container)
        recyclerView.adapter = EventScoreAdapter(EventScore("Champ"), this, findViewById(R.id.lap_score))

//        lapScore = findViewById(R.id.lap_score);
//        initScoreButtons(R.id.lap_score_container)
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
                it.setOnClickListener { updateLapPoints() }
            }
    }

    private fun updateLapPoints() {
        var cleans = 0
        val points = sectionScores
            .asSequence()
            .map { it.checkedRadioButtonId }
            .filter { it != -1 }
            .map { findViewById<RadioButton>(it) }
            .map {
                val score = it.tooltipText.toString().toInt()
                if (score == 0) cleans++
                score
            }
            .sum()
        lapScore.text = getString(R.string.lap_score, cleans, points)
    }
}