package net.yakavenka.trialsscore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.yakavenka.trialsscore.databinding.FragmentPointsEntryBinding
import net.yakavenka.trialsscore.model.SectionScoreAdapter
import net.yakavenka.trialsscore.viewmodel.ScoreCardViewModel


private const val TAG = "PointsEntryFragment"
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
            (activity?.application as TrialsScoreApplication).database.riderScoreDao(),
            (activity?.application as TrialsScoreApplication).sharedPreferences
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.points_entry_fragment_menu, menu)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clean_rider_scores -> {
                clearResults()
                true
            }
            R.id.action_edit_rider -> {
                val action = PointsEntryFragmentDirections.actionPointsEntryFragmentToEditRiderFragment(
                    title = getString(R.string.edit_rider_info),
                    riderId = navigationArgs.riderId
                )
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SectionScoreAdapter { sectionScore ->
            scoreCardViewModel.updateSectionScore(sectionScore)
        }
        binding.lapScoreContainer.adapter = adapter

        scoreCardViewModel.fetchScores(navigationArgs.riderId)

        scoreCardViewModel.sectionScores.observe(viewLifecycleOwner) { scoreSet ->
            Log.d(TAG, "Loaded ScoreCard $scoreSet")

            scoreSet.let { adapter.submitList(scoreSet.sectionScores) }
            binding.lapScore.text =
                getString(R.string.lap_score, scoreSet.getPoints(), scoreSet.getCleans())
        }
    }

    private fun clearResults() {
        Log.d("PointsEntryFragment", "Clearing results")
        scoreCardViewModel.clearScores(navigationArgs.riderId)
        val action = PointsEntryFragmentDirections.actionPointsEntryFragmentToEventScoreFragment()
        findNavController().navigate(action)
    }
}