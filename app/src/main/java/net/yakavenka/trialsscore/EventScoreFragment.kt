package net.yakavenka.trialsscore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.databinding.FragmentEventScoreBinding
import net.yakavenka.trialsscore.model.RiderScoreAdapter

class EventScoreFragment : Fragment() {

    private var _binding: FragmentEventScoreBinding? = null
    private val binding get() = _binding!!

    private val scoreDao: RiderScoreDao by lazy {
        (activity?.application as TrialsScoreApplication).database.riderScoreDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventScoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RiderScoreAdapter({})
        binding.recyclerView.adapter = adapter

        scoreDao.getAll().asLiveData().observe(viewLifecycleOwner) { scores ->
            adapter.submitList(scores)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}