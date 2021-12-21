package net.yakavenka.trialsscore.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.R

class EventScoreAdapter: RecyclerView.Adapter<EventScoreAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.lap_score_item, parent, false)
        return ViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.lapNumber.text = (position + 1).toString()
    }

    override fun getItemCount(): Int {
        return 20
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val lapNumber: TextView = view.findViewById(R.id.lap_number)
    }
}