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

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.openpgp_identity_settings, rootKey)

        val accountUuid = arguments!!.getString(ARGUMENT_ACCOUNT_UUID)
        val identityIndex = arguments!!.getInt(ARGUMENT_IDENTITY_INDEX)
        account = preferences.getAccount(accountUuid)
        identity = account.identities[identityIndex]

        findPreference<Preference>("openpgp_fingerprint")!!.summary = identity.openPgpKey.toString()

        masterSwitch.onPreferenceChangeListener = OnMasterSwitchChangeListener { newValue ->
            updatePreferences(newValue)
            true
        }

        updatePreferences(masterSwitch.isChecked)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = identity.email
    }

    private fun updatePreferences(checked: Boolean) {
        findPreference<Preference>("cat_enabled")!!.isVisible = checked
        findPreference<Preference>("cat_disabled")!!.isVisible = !checked

        val newIdentity = identity.copy(openPgpEnabled = checked)
        account.identities[arguments!!.getInt(ARGUMENT_IDENTITY_INDEX)] = newIdentity
        preferences.saveAccount(account)
    }

    companion object {
        val ARGUMENT_ACCOUNT_UUID = "account_uuid"
        val ARGUMENT_IDENTITY_INDEX = "identity_index"
    }
}