package net.yakavenka.trialsscore

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import net.yakavenka.trialsscore.components.EditRiderScreen
import net.yakavenka.trialsscore.databinding.FragmentEditRiderBinding
import net.yakavenka.trialsscore.ui.theme.AppTheme
import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel

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
                            val action = EditRiderFragmentDirections.actionEditRiderFragmentToEventScoreFragment()
                            findNavController().navigate(action)
                        }
                    )
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Hide keyboard.
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
        _binding = null
    }
}