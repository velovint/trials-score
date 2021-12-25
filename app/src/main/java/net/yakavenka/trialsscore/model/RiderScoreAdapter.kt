package net.yakavenka.trialsscore.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.databinding.RiderScoreItemBinding


class RiderScoreAdapter(
    private val onClick: (EventScore) -> Unit
) : ListAdapter<RiderScore, RiderScoreAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RiderScoreItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: RiderScoreItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(riderScore: RiderScore) {
            binding.apply {
                riderName.text = riderScore.riderName
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
