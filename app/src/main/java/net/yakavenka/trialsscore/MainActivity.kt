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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.lap_score_container)
        recyclerView.adapter = EventScoreAdapter(
            EventScore("Champ"),
            this,
            findViewById(R.id.lap_score))

    }
}