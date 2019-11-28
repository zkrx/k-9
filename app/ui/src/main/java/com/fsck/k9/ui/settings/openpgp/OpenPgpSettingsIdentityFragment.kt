package com.fsck.k9.ui.settings.openpgp


import android.os.Bundle
import androidx.preference.Preference

import com.fsck.k9.ui.R
import com.takisoft.preferencex.PreferenceFragmentCompatMasterSwitch

class OpenPgpSettingsIdentityFragment : PreferenceFragmentCompatMasterSwitch() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String) {
        setPreferencesFromResource(R.xml.openpgp_identity_settings, rootKey)

        masterSwitch.onPreferenceChangeListener = OnMasterSwitchChangeListener { newValue ->
            updatePreferences(newValue)
            true
        }

        updatePreferences(masterSwitch.isChecked)
    }

    private fun updatePreferences(enabled: Boolean) {
        findPreference<Preference>("cat_enabled")!!.setVisible(enabled)
        findPreference<Preference>("cat_disabled")!!.setVisible(!enabled)
    }
}