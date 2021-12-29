package net.yakavenka.trialsscore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.yakavenka.trialsscore.databinding.FragmentEditRiderBinding

/**
 * [Fragment] to Add/Edit rider info
 */
class EditRiderFragment : Fragment() {

    private var _binding: FragmentEditRiderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditRiderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}