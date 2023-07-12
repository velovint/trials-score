package net.yakavenka.trialsscore

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class EventSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}