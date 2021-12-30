package net.yakavenka.trialsscore.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.databinding.RiderScoreItemBinding


class RiderScoreAdapter(
    private val onClick: (RiderScoreAggregate) -> Unit
) : ListAdapter<RiderScoreAggregate, RiderScoreAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RiderScoreItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.itemView.setOnClickListener { onClick(currentItem)}
        holder.bind(currentItem)
    }

    class ViewHolder(private val binding: RiderScoreItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(scoreCard: RiderScoreAggregate) {
            binding.apply {
                val sectionScores = SectionScore.Set(scoreCard.sections)
                riderName.text = scoreCard.riderName
                riderScore.text = root.resources.getString(R.string.lap_score, sectionScores.getPoints(), sectionScores.getCleans())
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RiderScoreAggregate>() {
            override fun areItemsTheSame(oldItem: RiderScoreAggregate, newItem: RiderScoreAggregate): Boolean {
                return oldItem.riderName == newItem.riderName
            }

            override fun areContentsTheSame(oldItem: RiderScoreAggregate, newItem: RiderScoreAggregate): Boolean {
                return false // TODO Fix
            }
        }
    }
}
