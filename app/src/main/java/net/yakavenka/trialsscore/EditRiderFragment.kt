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
import androidx.navigation.fragment.navArgs
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import net.yakavenka.trialsscore.databinding.FragmentEditRiderBinding
import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel

/**
 * [Fragment] to Add/Edit rider info
 */
class EditRiderFragment : Fragment() {

    private val navigationArgs: EditRiderFragmentArgs by navArgs()

    private var _binding: FragmentEditRiderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditRiderViewModel by viewModels {
        EditRiderViewModel.Factory(
            (activity?.application as TrialsScoreApplication).database.riderScoreDao()
        )
    }

    private val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository((activity?.application as TrialsScoreApplication).sharedPreferences)
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
            .apply { addAll(userPreferencesRepository.fetchPreferences().riderClasses) }
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.riderClass.setAdapter(adapter)
        }

        val id = navigationArgs.riderId
        if (id > 0) {
            viewModel.loadRider(id).observe(viewLifecycleOwner) { bind(it) }
        } else {
            binding.saveAction.setOnClickListener { addRider() }
        }
    }

    private fun bind(rider: RiderScore) {
        binding.apply {
            riderName.setText(rider.name)
            riderClass.setText(rider.riderClass, false)
            saveAction.setOnClickListener { updateRider() }
        }
    }

    private fun updateRider() {
        viewModel.updateRider(
            navigationArgs.riderId,
            binding.riderName.text.toString(),
            binding.riderClass.text.toString()
        )
        val action = EditRiderFragmentDirections.actionEditRiderFragmentToPointsEntryFragment(
            riderId = navigationArgs.riderId, riderName = binding.riderName.text.toString())
        findNavController().navigate(action)
    }

    private fun addRider() {
        viewModel.addRider(
            binding.riderName.text.toString(),
            binding.riderClass.text.toString()
        )
        val action = EditRiderFragmentDirections.actionEditRiderFragmentToEventScoreFragment()
        findNavController().navigate(action)
    }
}