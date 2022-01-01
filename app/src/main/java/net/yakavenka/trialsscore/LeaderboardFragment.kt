package net.yakavenka.trialsscore

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.yakavenka.trialsscore.databinding.FragmentLeaderboardBinding
import net.yakavenka.trialsscore.model.RiderScoreAdapter
import net.yakavenka.trialsscore.viewmodel.EventScoreViewModel

private const val TAG = "LeaderboardFragment"

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    private val eventScores: EventScoreViewModel by activityViewModels {
        EventScoreViewModel.Factory(
            (activity?.application as TrialsScoreApplication).database.riderScoreDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_menu, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_results -> {
                Log.d(TAG, "exporting results")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RiderScoreAdapter {
            Log.d("EventScoreFragment", "Clicked on $it")
            val action = LeaderboardFragmentDirections.actionEventScoreFragmentToPointsEntryFragment(it.riderId)
            findNavController().navigate(action)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)

        eventScores.allScores.observe(viewLifecycleOwner) { scores ->
//            Log.d("EventScoreFragment", "Got scores " + scores)
            scores.let { adapter.submitList(scores) }
        }

        binding.floatingActionButton.setOnClickListener {
            val action = LeaderboardFragmentDirections.actionEventScoreFragmentToEditRiderFragment()
            findNavController().navigate(action)
        }
    }
}