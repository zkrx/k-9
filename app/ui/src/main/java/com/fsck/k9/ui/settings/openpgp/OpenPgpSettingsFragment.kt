package com.fsck.k9.ui.settings.openpgp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.withArguments
import com.takisoft.preferencex.PreferenceCategory
import com.takisoft.preferencex.PreferenceFragmentCompat
import com.takisoft.preferencex.SwitchPreferenceCompat
import com.xwray.groupie.Section
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class OpenPgpSettingsFragment : PreferenceFragmentCompat() {
    private val preferences: Preferences by inject()

    private val viewModel: OpenPgpSettingsViewModel by viewModel()

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.openpgp_settings, rootKey)

        populateIdentitiesList()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = preferenceScreen.title
    }

    private fun populateIdentitiesList() {
        viewModel.accounts.observeNotNull(this) { accounts ->
            if (accounts.isNotEmpty()) {
                populateIdentitiesList(accounts)
            }
        }
    }

    private fun populateIdentitiesList(accounts: List<Account>) {
        val dynamicCategory = findPreference<PreferenceCategory>("openpgp_identities")!!

        dynamicCategory.removeAll()

        val accountsSection = Section()
        for (account in accounts) {
            val accountIdentitiesSection = Section().apply {
                for ((identityIndex, identity) in account.identities.withIndex()) {
                    addIdentityPreference(account, identityIndex, identity, dynamicCategory)
                }
            }
            accountsSection.add(accountIdentitiesSection)
        }
    }

    private fun addIdentityPreference(account: Account, identityIndex: Int, identity: Identity, preferenceCategory: PreferenceCategory) {
        val ctx = preferenceManager.context // this is the material styled context

        val newPreference = SwitchPreferenceCompat(ctx)
        newPreference.fragment = "unused" // hack to enable separator in com.takisoft.preferencex.SwitchPreferenceCompat
        newPreference.title = identity.email
        newPreference.isChecked = identity.openPgpEnabled
        newPreference.summaryOff = getString(R.string.settings_openpgp_mode_disabled)

        newPreference.intent = Intent().apply {
            putExtra("account_uuid", account.uuid)
            putExtra("identity_index", identityIndex)
        }

        val summaryOn = when {
            identity.openPgpModeMutual -> R.string.settings_openpgp_mode_automatic
            else -> R.string.settings_openpgp_mode_manual
        }
        newPreference.summaryOn = getString(summaryOn)

        newPreference.setOnPreferenceChangeListener { preference, newValue ->
            onIdentityChecked(preference, newValue as Boolean)
            true
        }

        newPreference.setOnPreferenceClickListener { preference ->
            Toast.makeText(requireActivity(), "click", Toast.LENGTH_SHORT).show()
//            val intent = preference.intent
            // TODO use intent to start activity

            true
        }

        preferenceCategory.addPreference(newPreference)
    }

    private fun onIdentityChecked(preference: Preference, checked: Boolean) {
        val account = preferences.getAccount(preference.intent.getStringExtra("account_uuid"))
        val identityIndex = preference.intent.getIntExtra("identity_index", -1)

        val newIdentity = account.identities[identityIndex].copy(openPgpEnabled = checked)
        account.identities[identityIndex] = newIdentity
        preferences.saveAccount(account)

        Toast.makeText(requireActivity(), "identity: " + newIdentity.email + "checked: " + checked, Toast.LENGTH_SHORT).show();
    }

    companion object {
        fun create(rootKey: String? = null) = OpenPgpSettingsFragment().withArguments(ARG_PREFERENCE_ROOT to rootKey)
    }
}
