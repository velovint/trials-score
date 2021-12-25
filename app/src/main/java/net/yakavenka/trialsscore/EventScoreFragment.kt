package net.yakavenka.trialsscore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.yakavenka.trialsscore.data.RiderScoreDao
import net.yakavenka.trialsscore.databinding.FragmentEventScoreBinding
import net.yakavenka.trialsscore.databinding.SectionScoreItemBinding
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
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)

        scoreDao.getAll().asLiveData().observe(viewLifecycleOwner) { scores ->
//            Log.d("EventScoreFragment", "Got scores " + scores)
            scores.let { adapter.submitList(scores) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}