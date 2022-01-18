package net.yakavenka.trialsscore

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import net.yakavenka.trialsscore.databinding.FragmentEditRiderBinding
import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel
import net.yakavenka.trialsscore.viewmodel.ScoreCardViewModel

/**
 * [Fragment] to Add/Edit rider info
 */
class EditRiderFragment : Fragment() {

    private var _binding: FragmentEditRiderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditRiderViewModel by viewModels {
        EditRiderViewModel.Factory(
            (activity?.application as TrialsScoreApplication).database.riderScoreDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditRiderBinding.inflate(inflater, container, false)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item)
            .apply { addAll(EditRiderViewModel.RIDER_CLASS_OPTIONS) }
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.riderClass.setAdapter(adapter)
        }

        binding.saveAction.setOnClickListener {
            viewModel.addRider(
                binding.riderName.text.toString(),
                binding.riderClass.text.toString()
            )
            val action = EditRiderFragmentDirections.actionEditRiderFragmentToEventScoreFragment()
            findNavController().navigate(action)
        }
    }
}