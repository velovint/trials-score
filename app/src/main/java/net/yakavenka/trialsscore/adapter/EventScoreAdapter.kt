package net.yakavenka.trialsscore.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.model.EventScore

class EventScoreAdapter(private val eventScore: EventScore, private val totalScoreView: TextView):
    RecyclerView.Adapter<EventScoreAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.section_score_item, parent, false)
        return ViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.lapNumber.text = (position + 1).toString()
        holder.scoreFive.setOnClickListener { updateScore(position, 5) }
    }

    private fun updateScore(section: Int, points: Int) {
        eventScore.sectionScores.set(section, points)
        totalScoreView.text = eventScore.getTotal().toString()
    }

    override fun getItemCount(): Int {
        return eventScore.sectionScores.size
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val lapNumber: TextView = view.findViewById(R.id.lap_number)
        val scoreZero: RadioButton = view.findViewById(R.id.section_score_0)
        val scoreOne: RadioButton = view.findViewById(R.id.section_score_1)
        val scoreTwo: RadioButton = view.findViewById(R.id.section_score_2)
        val scoreThree: RadioButton = view.findViewById(R.id.section_score_3)
        val scoreFive: RadioButton = view.findViewById(R.id.section_score_5)
    }
}