package net.yakavenka.trialsscore.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.databinding.RiderScoreItemBinding


class RiderScoreAdapter(
    private val onClick: (RiderScore) -> Unit
) : ListAdapter<RiderScore, RiderScoreAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RiderScoreItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.itemView.setOnClickListener { onClick(currentItem)}
        holder.bind(currentItem)
    }

    class ViewHolder(private val binding: RiderScoreItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(scoreCard: RiderScore) {
            binding.apply {
                riderName.text = scoreCard.riderName
                riderScore.text = root.resources.getString(R.string.lap_score, scoreCard.getCleans(), scoreCard.getPoints())
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RiderScore>() {
            override fun areItemsTheSame(oldItem: RiderScore, newItem: RiderScore): Boolean {
                return oldItem.riderName == newItem.riderName
            }

            override fun areContentsTheSame(oldItem: RiderScore, newItem: RiderScore): Boolean {
                return false // TODO Fix
            }
        }
    }
}
