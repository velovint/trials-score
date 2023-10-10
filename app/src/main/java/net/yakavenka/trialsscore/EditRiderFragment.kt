package net.yakavenka.trialsscore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import net.yakavenka.trialsscore.components.EditRiderScreen
import net.yakavenka.trialsscore.databinding.FragmentEditRiderBinding
import net.yakavenka.trialsscore.ui.theme.AppTheme
import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel

private const val TAG = "EditRiderFragment"

/**
 * [Fragment] to Add/Edit rider info
 */
class EditRiderFragment : Fragment() {

    private val navigationArgs: EditRiderFragmentArgs by navArgs()

    private var _binding: FragmentEditRiderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditRiderViewModel by viewModels { EditRiderViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditRiderBinding.inflate(inflater, container, false)
        binding.composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                // In Compose world
                AppTheme {
                    EditRiderScreen(
                        viewModel = viewModel,
                        onSave = {
                            viewModel.saveRider()
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = navigationArgs.riderId
        Log.d(TAG, "Edit Rider id: $id")
        if (id > 0) {
            viewModel.loadRider(id)
        }
    }
}