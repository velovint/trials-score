package net.yakavenka.trialsscore.model

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.data.SectionScore
import net.yakavenka.trialsscore.databinding.SectionScoreItemBinding

class SectionScoreAdapter(
    private val dataSet: List<SectionScore>,
    private val onChangeCallback: (SectionScore) -> Unit
) : RecyclerView.Adapter<SectionScoreAdapter.ViewHolder>()  {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SectionScoreItemBinding.inflate(LayoutInflater.from(parent.context))
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position], onChangeCallback)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    class ViewHolder(private val binding: SectionScoreItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(sectionScore: SectionScore, onChangeCallback: (SectionScore) -> Unit) {
            binding.apply {
                lapNumber.text = sectionScore.sectionNumber.toString()
                bindRadio(sectionScore0, 0, sectionScore, onChangeCallback)
                bindRadio(sectionScore1, 1, sectionScore, onChangeCallback)
                bindRadio(sectionScore2, 2, sectionScore, onChangeCallback)
                bindRadio(sectionScore3, 3, sectionScore, onChangeCallback)
                bindRadio(sectionScore5, 5, sectionScore, onChangeCallback)
            }
        }

        private fun bindRadio(
            pointsButtn: RadioButton,
            numPoints: Int,
            sectionScore: SectionScore,
            onChangeCallback: (SectionScore) -> Unit
        ) {
            pointsButtn.isChecked = sectionScore.points == numPoints
            pointsButtn.setOnClickListener { onChangeCallback(sectionScore.copy(points = numPoints)) }
        }

    }
}