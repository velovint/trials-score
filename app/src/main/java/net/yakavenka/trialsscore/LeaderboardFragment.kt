package net.yakavenka.trialsscore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
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
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            eventScores.exportReport(uri, requireContext().contentResolver)
        }
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
                initDownload(Uri.parse("Downloads"))
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

    private fun initDownload(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "report.txt")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivity(intent)
    }

}
