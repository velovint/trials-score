package net.yakavenka.trialsscore.model

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.R
import net.yakavenka.trialsscore.databinding.RiderScoreItemBinding
import net.yakavenka.trialsscore.databinding.RiderScoreItemWithHeaderBinding
import net.yakavenka.trialsscore.viewmodel.RiderStanding

private const val TAG = "EventScoreFragment"

class RiderScoreAdapter(
    private val onClick: (RiderStanding) -> Unit
) : ListAdapter<RiderStanding, RiderScoreAdapter.ViewHolder>(DIFF_CALLBACK) {

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
        abstract fun bind(scoreCard: RiderStanding)

        fun getFormattedRiderStanding(scoreCard: RiderStanding): CharSequence {
            // https://unicode.org/charts/nameslist/c_2800.html
            if (scoreCard.isFinished()) return scoreCard.standing.toString()
            else if (scoreCard.getProgress() > 60) return "⠅"
            else if (scoreCard.getProgress() > 30) return "⠂"
            else return "*"
        }
    }

    class ViewHolderNoHeader(
        private val binding: RiderScoreItemBinding
    ) : ViewHolder(binding.root) {

        override fun bind(scoreCard: RiderStanding) {
            binding.apply {
                riderStanding.text = getFormattedRiderStanding(scoreCard)
                riderName.text = scoreCard.riderName
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

        override fun bind(scoreCard: RiderStanding) {
            binding.apply {
                riderNameHeader.text = scoreCard.riderClass
                riderStanding.text = getFormattedRiderStanding(scoreCard)
                riderName.text = scoreCard.riderName
                riderScore.text = root.resources.getString(
                    R.string.lap_score,
                    scoreCard.points,
                    scoreCard.numCleans
                )
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RiderStanding>() {
            override fun areItemsTheSame(
                oldItem: RiderStanding,
                newItem: RiderStanding
            ): Boolean {
                return oldItem.riderId == newItem.riderId
            }

            override fun areContentsTheSame(
                oldItem: RiderStanding,
                newItem: RiderStanding
            ): Boolean {
                return compareBy(
                        RiderStanding::riderName,
                        RiderStanding::riderClass,
                        RiderStanding::points,
                        RiderStanding::numCleans
                    ).compare(oldItem, newItem) == 0
            }
        }
    }
}
