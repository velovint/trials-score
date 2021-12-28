package net.yakavenka.trialsscore.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.databinding.SectionScoreItemBinding
import net.yakavenka.trialsscore.model.EventScore

class EventScoreAdapter(
    private val eventScore: EventScore,
    private val context: Context,
    private val totalScoreView: TextView
) : RecyclerView.Adapter<EventScoreAdapter.ViewHolder>() {
        init {
            updateTotal()
        }

    private fun updateTotal() {
        totalScoreView.text = context.resources.getString(
            R.string.lap_score,
            eventScore.getCleans(),
            eventScore.getTotalPoints()
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            SectionScoreItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            lapNumber.text = (position + 1).toString()
            sectionScore0.setOnClickListener { updateScore(position, 0) }
            sectionScore1.setOnClickListener { updateScore(position, 1) }
            sectionScore2.setOnClickListener { updateScore(position, 2) }
            sectionScore3.setOnClickListener { updateScore(position, 3) }
            sectionScore5.setOnClickListener { updateScore(position, 5) }
        }
    }

    override fun getItemCount(): Int {
        return eventScore.sectionScores.size
    }

    private fun updateScore(section: Int, points: Int) {
        eventScore.sectionScores[section] = points
        updateTotal()
    }

    class ViewHolder(
        val binding: SectionScoreItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

    }
}