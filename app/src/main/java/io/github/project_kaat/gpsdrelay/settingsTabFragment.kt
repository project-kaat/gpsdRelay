package io.github.project_kaat.gpsdrelay

import android.os.Bundle

import androidx.preference.PreferenceFragmentCompat

class settingsTabFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}