package net.yakavenka.trialsscore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import net.yakavenka.trialsscore.components.LoopScoreEntryScreen
import net.yakavenka.trialsscore.databinding.FragmentPointsEntryBinding
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

    private val scoreCardViewModel: ScoreCardViewModel by viewModels {ScoreCardViewModel.Factory }

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
        binding.composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                // In Compose world
                MaterialTheme {
                    LoopScoreEntryScreen(
                        scoreCardViewModel,
                        onNavigate = { loopNum ->
                            val action = PointsEntryFragmentDirections.actionPointsEntryFragmentSelf(
                                riderId = navigationArgs.riderId,
                                loop = loopNum,
                                riderName = navigationArgs.riderName
                            )
                            findNavController().navigate(action)
                        })
                }
            }

        }
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
                val action =
                    PointsEntryFragmentDirections.actionPointsEntryFragmentToEditRiderFragment(
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

        scoreCardViewModel.fetchScores(navigationArgs.riderId, navigationArgs.loop)
    }

    private fun clearResults() {
        Log.d("PointsEntryFragment", "Clearing results")
        scoreCardViewModel.clearScores(navigationArgs.riderId)
        val action = PointsEntryFragmentDirections.actionPointsEntryFragmentToEventScoreFragment()
        findNavController().navigate(action)
    }
}