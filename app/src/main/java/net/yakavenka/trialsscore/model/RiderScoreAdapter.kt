package net.yakavenka.trialsscore.model

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.databinding.FragmentEventScoreBinding


class RiderScoreAdapter(
    private val onClick: (EventScore) -> Unit
) : ListAdapter<RiderScore, RiderScoreAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(binding: FragmentEventScoreBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        TODO("Not yet implemented")
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
