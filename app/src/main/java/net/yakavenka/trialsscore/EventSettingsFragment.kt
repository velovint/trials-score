package net.yakavenka.trialsscore

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import net.yakavenka.trialsscore.data.UserPreferencesRepository

class EventSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val numSections: EditTextPreference? = findPreference(UserPreferencesRepository.NUM_SECTIONS_KEY)
        numSections?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }
}