package net.yakavenka.trialsscore.model

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.data.RiderScoreAggregate
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.databinding.RiderScoreItemBinding
import net.yakavenka.trialsscore.databinding.RiderScoreItemWithHeaderBinding

private const val TAG = "EventScoreFragment"

class RiderScoreAdapter(
    private val onClick: (RiderScoreSummary) -> Unit
) : ListAdapter<RiderScoreSummary, RiderScoreAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "Creating view of type $viewType")

        if (viewType == 1) {
            return ViewHolderWithHeader(
                RiderScoreItemWithHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
        return ViewHolderNoHeader(
            RiderScoreItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.itemView.setOnClickListener { onClick(currentItem) }
        holder.bind(currentItem)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isFirstInClass(position)) 1 else 0
    }

    private fun isFirstInClass(position: Int): Boolean {
        if (position == 0) return true
        val prevItem = getItem(position - 1)
        val thisItem = getItem(position)
        if (prevItem.riderClass != thisItem.riderClass) return true
        return false
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(scoreCard: RiderScoreSummary)
        fun getFormattedRiderName(scoreCard: RiderScoreSummary): String {
            return if (scoreCard.isFinished())
                scoreCard.riderName
                else scoreCard.riderName + " *"
        }
    }

    class ViewHolderNoHeader(
        private val binding: RiderScoreItemBinding
    ) : ViewHolder(binding.root) {

        override fun bind(scoreCard: RiderScoreSummary) {
            binding.apply {
                riderName.text = getFormattedRiderName(scoreCard)
                riderScore.text = root.resources.getString(
                    R.string.lap_score,
                    scoreCard.points,
                    scoreCard.numCleans
                )
            }
        }
    }

    class ViewHolderWithHeader(
        private val binding: RiderScoreItemWithHeaderBinding
    ) : ViewHolder(binding.root) {

        override fun bind(scoreCard: RiderScoreSummary) {
            binding.apply {
                riderNameHeader.text = scoreCard.riderClass
                riderName.text = getFormattedRiderName(scoreCard)
                riderScore.text = root.resources.getString(
                    R.string.lap_score,
                    scoreCard.points,
                    scoreCard.numCleans
                )
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RiderScoreSummary>() {
            override fun areItemsTheSame(
                oldItem: RiderScoreSummary,
                newItem: RiderScoreSummary
            ): Boolean {
                return oldItem.riderName == newItem.riderName
            }

            override fun areContentsTheSame(
                oldItem: RiderScoreSummary,
                newItem: RiderScoreSummary
            ): Boolean {
                return false // TODO Fix
            }
        }
    }
}
