package com.fsck.k9.ui.settings.openpgp


import android.os.Bundle
import androidx.preference.Preference
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences

import com.fsck.k9.ui.R
import com.takisoft.preferencex.PreferenceFragmentCompatMasterSwitch
import org.koin.android.ext.android.inject

class OpenPgpSettingsIdentityFragment : PreferenceFragmentCompatMasterSwitch() {
    private val preferences: Preferences by inject()

    lateinit var account: Account
    lateinit var identity: Identity
    var identityIndex: Int? = null

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.openpgp_identity_settings, rootKey)

        val accountUuid = arguments!!.getString(ARGUMENT_ACCOUNT_UUID)
        identityIndex = arguments!!.getInt(ARGUMENT_IDENTITY_INDEX)
        account = preferences.getAccount(accountUuid)
        identity = account.identities[identityIndex!!]

        masterSwitch.isChecked = identity.openPgpEnabled

        masterSwitch.onPreferenceChangeListener = OnMasterSwitchChangeListener { newValue ->
            updatePreferences(newValue)

            identity = identity.copy(openPgpEnabled = newValue)
            saveIdentity()
            true
        }

        val openPgpFinerprint = findPreference<Preference>("openpgp_fingerprint")!!
        openPgpFinerprint.summary = identity.openPgpKey.toString()

        val openPgpMutualMode = findPreference<Preference>("openpgp_mutual_mode")!!
        openPgpMutualMode.setOnPreferenceChangeListener { preference, newValue ->
            identity = identity.copy(openPgpModeMutual = newValue as Boolean)
            saveIdentity()

            true
        }

        updatePreferences(masterSwitch.isChecked)
    }

    private fun updatePreferences(checked: Boolean) {
        findPreference<Preference>("cat_enabled")!!.isVisible = checked
        findPreference<Preference>("cat_disabled")!!.isVisible = !checked
    }

    private fun saveIdentity() {
        val identities = account.identities
        if (identityIndex == -1) {
            identities.add(identity)
        } else {
            identities.removeAt(identityIndex!!)
            identities.add(identityIndex!!, identity)
        }

        preferences.saveAccount(account)
    }

    companion object {
        val ARGUMENT_ACCOUNT_UUID = "account_uuid"
        val ARGUMENT_IDENTITY_INDEX = "identity_index"
    }
}