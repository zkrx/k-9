package com.fsck.k9.ui.crypto.keyimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.crypto.openpgp.SecretKeyInfo
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observeNotNull
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.fragment_settings_import.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SecretKeyImportFragment : Fragment() {
    private val viewModel: SecretKeyImportViewModel by viewModel()

    private lateinit var settingsImportAdapter: FastAdapter<ImportListItem>
    private lateinit var itemAdapter: ItemAdapter<ImportListItem>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_secret_key_import, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            viewModel.initializeFromSavedState(savedInstanceState)
        }

        initializeSettingsImportList(view)
        pickDocumentButton.setOnClickListener { viewModel.onPickDocumentButtonClicked() }
        importButton.setOnClickListener { viewModel.onImportButtonClicked() }
        closeButton.setOnClickListener { viewModel.onCloseButtonClicked() }

        viewModel.getUiState().observeNotNull(this) { updateUi(it) }
        viewModel.getActionEvents().observeNotNull(this) { handleActionEvents(it) }
    }

    private fun initializeSettingsImportList(view: View) {
        itemAdapter = ItemAdapter()
        settingsImportAdapter = FastAdapter.with(itemAdapter).apply {
            setHasStableIds(true)
            onClickListener = { _, _, _, position ->
                viewModel.onSettingsListItemClicked(position)
                true
            }
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.settingsImportList)
        recyclerView.adapter = settingsImportAdapter
    }

    private fun updateUi(uiState: SecretKeyImportUiState) {
        when (uiState.importButton) {
            ButtonState.DISABLED -> {
                importButton.isVisible = true
                importButton.isEnabled = false
            }
            ButtonState.ENABLED -> {
                importButton.isVisible = true
                importButton.isEnabled = true
            }
            ButtonState.INVISIBLE -> importButton.isInvisible = true
            ButtonState.GONE -> importButton.isGone = true
        }

        closeButton.isGone = uiState.closeButton == ButtonState.GONE
        when (uiState.closeButtonLabel) {
            CloseButtonLabel.OK -> closeButton.setText(R.string.okay_action)
            CloseButtonLabel.LATER -> closeButton.setText(R.string.settings_import_later_button)
        }

        settingsImportList.isVisible = uiState.isSettingsListVisible
        pickDocumentButton.isInvisible = !uiState.isPickDocumentButtonVisible
        pickDocumentButton.isEnabled = uiState.isPickDocumentButtonEnabled
        loadingProgressBar.isVisible = uiState.isLoadingProgressVisible
        importProgressBar.isVisible = uiState.isImportProgressVisible

        statusText.isVisible = uiState.statusText != StatusText.HIDDEN
        when (uiState.statusText) {
            StatusText.IMPORTING_PROGRESS -> {
                statusText.text = getString(R.string.settings_importing)
            }
            StatusText.IMPORT_SUCCESS -> {
                statusText.text = getString(R.string.settings_import_success_generic)
            }
            StatusText.IMPORT_SUCCESS_PASSWORD_REQUIRED -> {
                statusText.text = getString(R.string.settings_import_password_required)
            }
            StatusText.IMPORT_READ_FAILURE -> {
                statusText.text = getString(R.string.settings_import_read_failure)
            }
            StatusText.IMPORT_PARTIAL_FAILURE -> {
                statusText.text = getString(R.string.settings_import_partial_failure)
            }
            StatusText.IMPORT_FAILURE -> {
                statusText.text = getString(R.string.settings_import_failure)
            }
            StatusText.HIDDEN -> statusText.text = null
        }

        setSettingsList(uiState.settingsList, uiState.isSettingsListEnabled)
    }

    // TODO: Update list instead of replacing it completely
    private fun setSettingsList(items: List<SecretKeyInfo>, enable: Boolean) {
        val importListItems = items.map { item ->
            ImportListItem(1, item.secretKeyRing.primaryUserIdWithFallback)
        }

        itemAdapter.set(importListItems)

        settingsImportList.isEnabled = enable
    }

    private fun handleActionEvents(action: Action) {
        when (action) {
            is Action.Close -> closeImportScreen()
            is Action.PickDocument -> pickDocument()
        }
    }

    private fun closeImportScreen() {
        findNavController().popBackStack()
    }

    private fun pickDocument() {
        val createDocumentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(createDocumentIntent, REQUEST_PICK_DOCUMENT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PICK_DOCUMENT -> handlePickDocumentResult(resultCode, data)
        }
    }

    private fun handlePickDocumentResult(resultCode: Int, data: Intent?) {
        val contentUri = data?.data
        if (resultCode == Activity.RESULT_OK && contentUri != null) {
            viewModel.onDocumentPicked(contentUri)
        } else {
            viewModel.onDocumentPickCanceled()
        }
    }

    companion object {
        private const val REQUEST_PICK_DOCUMENT = Activity.RESULT_FIRST_USER
    }
}
