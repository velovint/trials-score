package net.yakavenka.trialsscore

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import net.yakavenka.trialsscore.adapter.EventScoreAdapter
import net.yakavenka.trialsscore.databinding.FragmentPointsEntryBinding
import net.yakavenka.trialsscore.model.EventScore
import net.yakavenka.trialsscore.model.SectionScoreAdapter
import net.yakavenka.trialsscore.viewmodel.EventScoreViewModel
import net.yakavenka.trialsscore.viewmodel.ScoreCardViewModel


/**
 * [Fragment] to enter rider points
 */
class PointsEntryFragment : Fragment() {
    private var _binding: FragmentPointsEntryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val navigationArgs: PointsEntryFragmentArgs by navArgs()

    private val scoreCardViewModel: ScoreCardViewModel by viewModels {
        ScoreCardViewModel.Factory(
            (activity?.application as TrialsScoreApplication).database.riderScoreDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPointsEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scoreCardViewModel.fetchScores(navigationArgs.riderId)

        scoreCardViewModel.sectionScores.observe(viewLifecycleOwner) { scoreSet ->
            Log.d("EventScoreFragment", "Loaded ScoreCard $scoreSet")
            val adapter = SectionScoreAdapter(scoreSet.sectionScores) { sectionScore ->
                scoreCardViewModel.updateSectionScore(sectionScore)
            }
            binding.lapScoreContainer.adapter = adapter
        }
        scoreCardViewModel.sectionScores.observe(viewLifecycleOwner) { sectionScores ->
            binding.lapScore.text =
                getString(R.string.lap_score, sectionScores.getPoints(), sectionScores.getCleans())
        }

        binding.clearPointsButton.setOnClickListener {
            scoreCardViewModel.clearScores(navigationArgs.riderId)
            val action = PointsEntryFragmentDirections.actionPointsEntryFragmentToEventScoreFragment()
            findNavController().navigate(action)
        }
    }

    fun clearResults() {
        Log.d("PointsEntryFragment", "Clearing results")
    }
}