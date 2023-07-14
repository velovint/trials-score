package net.yakavenka.trialsscore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
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
            (activity?.application as TrialsScoreApplication).database.riderScoreDao(),
            (activity?.application as TrialsScoreApplication).sharedPreferences
        )
    }

    private val exportPrompt: ActivityResultLauncher<String> = registerExportPrompt()

    private val importPrompt: ActivityResultLauncher<Array<String>> = registerImportPrompt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    // can't find much documentation to replace deprecated method
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
        val navController = findNavController()

        return when (item.itemId) {
            R.id.action_export_results -> {
                Log.d(TAG, "init download")
                exportPrompt.launch("report.csv", )
                true
            }
            R.id.action_import_riders -> {
                Log.d(TAG, "import riders")
                importPrompt.launch(arrayOf("text/*"))
                true
            }
            R.id.clear_all_data -> {
                eventScores.clearAll()
                true
            }
            else -> item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = RiderScoreAdapter {
            Log.d("EventScoreFragment", "Clicked on $it")
            val action = LeaderboardFragmentDirections
                .actionEventScoreFragmentToPointsEntryFragment(it.riderId, it.riderName)
            findNavController().navigate(action)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)

        eventScores.allScores.observe(viewLifecycleOwner) { scores ->
//            Log.d("EventScoreFragment", "Got scores " + scores)
            scores.let { adapter.submitList(scores) }
        }

        binding.floatingActionButton.setOnClickListener {
            val action = LeaderboardFragmentDirections.actionEventScoreFragmentToEditRiderFragment(
                title = getString(R.string.add_new_rider))
            findNavController().navigate(action)
        }
    }

    private fun registerExportPrompt(): ActivityResultLauncher<String> {
        val contract = object : ActivityResultContracts.CreateDocument("text/csv") {
            override fun createIntent(context: Context, input: String): Intent {
                return super.createIntent(context, input).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    // TODO do I still need this?
                    type = "text/csv"
                    // Optionally, specify a URI for the directory that should be opened in
                    // the system file picker before your app creates the document.
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "Downloads")
                }
            }
        }
        return registerForActivityResult(contract) { uri ->
            if (uri == null) {
                Log.i(TAG, "Export file is not selected. Skipping.")
                return@registerForActivityResult
            }
            eventScores.exportReport(uri, requireContext().contentResolver)
        }
    }

    private fun registerImportPrompt(): ActivityResultLauncher<Array<String>> {
        val contract = object : ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: Context, input: Array<String>): Intent {
                return super.createIntent(context, input).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, "Downloads")
                }
            }
        }
        return registerForActivityResult(contract) { uri ->
            if (uri == null) {
                Log.i(TAG, "Import file is not selected. Skipping.")
                return@registerForActivityResult
            }
            eventScores.importRiders(uri, requireContext().contentResolver)
        }
    }

}
