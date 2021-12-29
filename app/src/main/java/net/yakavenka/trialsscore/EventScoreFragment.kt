package net.yakavenka.trialsscore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import net.yakavenka.trialsscore.databinding.FragmentEventScoreBinding
import net.yakavenka.trialsscore.model.RiderScoreAdapter
import net.yakavenka.trialsscore.viewmodel.EventScoreViewModel

class EventScoreFragment : Fragment() {

    private var _binding: FragmentEventScoreBinding? = null
    private val binding get() = _binding!!

    private val eventScores: EventScoreViewModel by activityViewModels {
        EventScoreViewModel.Factory(
            (activity?.application as TrialsScoreApplication).database.riderScoreDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventScoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RiderScoreAdapter {
            Log.d("EventScoreFragment", "Clicked on " + it)
            val action = EventScoreFragmentDirections.actionEventScoreFragmentToPointsEntryFragment(it.riderId)
            findNavController().navigate(action)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)

        eventScores.allScores.observe(viewLifecycleOwner) { scores ->
//            Log.d("EventScoreFragment", "Got scores " + scores)
            scores.let { adapter.submitList(scores) }
        }

        binding.floatingActionButton.setOnClickListener {
            val action = EventScoreFragmentDirections.actionEventScoreFragmentToEditRiderFragment()
            findNavController().navigate(action)
        }
    }
}