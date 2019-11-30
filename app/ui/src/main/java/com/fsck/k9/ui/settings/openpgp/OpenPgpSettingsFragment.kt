package com.fsck.k9.ui.settings.openpgp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.fsck.k9.ui.R
import com.fsck.k9.ui.endtoend.AutocryptKeyTransferActivity
import com.fsck.k9.ui.observeNotNull
import com.fsck.k9.ui.settings.general.GeneralSettingsDataStore
import com.fsck.k9.ui.withArguments
import com.takisoft.preferencex.PreferenceCategory
import com.takisoft.preferencex.PreferenceFragmentCompat
import com.takisoft.preferencex.SwitchPreferenceCompat
import com.xwray.groupie.Section
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.sufficientlysecure.keychain.ui.MainActivity

// TODO: add preferences once and then only update the actual values to prevent flickering
class OpenPgpSettingsFragment : PreferenceFragmentCompat() {
    private val preferences: Preferences by inject()
    private val dataStore: GeneralSettingsDataStore by inject()

    private val viewModel: OpenPgpSettingsViewModel by viewModel()

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore
        setPreferencesFromResource(R.xml.openpgp_settings, rootKey)

        populateIdentitiesList()
        initPreferenceButtons()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dataStore.activity = activity
    }

    private fun initPreferenceButtons() {
        findPreference<Preference>("secret_key_import")!!.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_settingsOpenPgpScreen_to_settingsOpenPgpSecretKeyImportScreen)
            true
        }

        findPreference<Preference>("autocrypt_transfer")!!.setOnPreferenceClickListener {
            // TODO: implement autocrypt transfer with ALL keys
            val intent = AutocryptKeyTransferActivity.createIntent(requireContext(), "XXX")
            startActivity(intent)

            true
        }

        findPreference<Preference>("openpgp_manage")!!.setOnPreferenceClickListener {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)

            true
        }
    }

    private fun populateIdentitiesList() {
        viewModel.accounts.observeNotNull(this) { accounts ->
            if (accounts.isNotEmpty()) {
                populateIdentitiesList(accounts)
            }
        }
    }

    private fun populateIdentitiesList(accounts: List<Account>) {
        val identityCategory = findPreference<PreferenceCategory>("openpgp_identities")!!

        identityCategory.removeAll()

        val accountsSection = Section()
        for (account in accounts) {
            val accountIdentitiesSection = Section().apply {
                for ((identityIndex, identity) in account.identities.withIndex()) {
                    addIdentityPreference(account, identityIndex, identity, identityCategory)
                }
            }
            accountsSection.add(accountIdentitiesSection)
        }
    }

    private fun addIdentityPreference(account: Account, identityIndex: Int, identity: Identity, preferenceCategory: PreferenceCategory) {
        val ctx = preferenceManager.context // this is the material styled context

        val newPreference = SwitchPreferenceCompat(ctx)
        newPreference.isPersistent = false
        newPreference.fragment = "unused" // hack to enable separator in com.takisoft.preferencex.SwitchPreferenceCompat
        newPreference.title = identity.email
        val summaryOn = when {
            identity.openPgpModeMutual -> R.string.settings_openpgp_mode_automatic
            else -> R.string.settings_openpgp_mode_manual
        }
        newPreference.summaryOn = getString(summaryOn)
        newPreference.summaryOff = getString(R.string.settings_openpgp_mode_disabled)

        newPreference.intent = Intent().apply {
            putExtra("account_uuid", account.uuid)
            putExtra("identity_index", identityIndex)
        }

        newPreference.setOnPreferenceChangeListener { preference, newValue ->
            onIdentityChecked(preference, newValue as Boolean)
            true
        }

        newPreference.setOnPreferenceClickListener { preference ->
            launchIdentitySettings(preference)
            true
        }

        preferenceCategory.addPreference(newPreference)
        newPreference.isChecked = identity.openPgpEnabled
    }

    private fun launchIdentitySettings(preference: Preference) {
        val arguments = Bundle().apply {
            val accountUuid = preference.intent.getStringExtra("account_uuid")
            val account = preferences.getAccount(accountUuid)
            val identityIndex = preference.intent.getIntExtra("identity_index", -1)
            val email = account.identities[identityIndex].email

            putString("email", email)
            putString(OpenPgpSettingsIdentityFragment.ARGUMENT_ACCOUNT_UUID, accountUuid)
            putInt(OpenPgpSettingsIdentityFragment.ARGUMENT_IDENTITY_INDEX, identityIndex)
        }
        findNavController().navigate(R.id.action_settingsOpenPgpScreen_to_settingsOpenPgpIdentityScreen, arguments)
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
