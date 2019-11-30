package com.fsck.k9.ui.crypto.keyimport

import com.fsck.k9.crypto.openpgp.SecretKeyInfo

class SecretKeyImportUiState {
    var settingsList: List<SecretKeyInfo> = emptyList()
    var isSettingsListVisible = false
    var isSettingsListEnabled = true
    var importButton: ButtonState = ButtonState.DISABLED
    var closeButton: ButtonState = ButtonState.GONE
    var closeButtonLabel: CloseButtonLabel = CloseButtonLabel.OK
    var isPickDocumentButtonVisible = true
    var isPickDocumentButtonEnabled = true
    var isLoadingProgressVisible = false
    var isImportProgressVisible = false
    var statusText = StatusText.HIDDEN

    val hasImportStarted
        get() = importButton == ButtonState.GONE

    val hasDocumentBeenRead
        get() = isSettingsListVisible

    fun enablePickDocumentButton() {
        isPickDocumentButtonEnabled = true
    }

    fun disablePickDocumentButton() {
        statusText = StatusText.HIDDEN
        isPickDocumentButtonEnabled = false
    }

    private fun enableImportButton() {
        importButton = ButtonState.ENABLED
        isImportProgressVisible = false
        isSettingsListEnabled = true
    }

    private fun disableImportButton() {
        importButton = ButtonState.DISABLED
        isImportProgressVisible = false
    }

    fun showLoadingProgress() {
        isLoadingProgressVisible = true
        isPickDocumentButtonVisible = false
        isSettingsListEnabled = false
        statusText = StatusText.HIDDEN
    }

    fun showImportingProgress() {
        isImportProgressVisible = true
        isSettingsListEnabled = false
        importButton = ButtonState.INVISIBLE
        statusText = StatusText.IMPORTING_PROGRESS
    }

    private fun showSuccessText() {
        importButton = ButtonState.GONE
        closeButton = ButtonState.ENABLED
        closeButtonLabel = CloseButtonLabel.OK
        isImportProgressVisible = false
        isSettingsListEnabled = true
        statusText = StatusText.IMPORT_SUCCESS
    }

    private fun showPasswordRequiredText() {
        importButton = ButtonState.GONE
        closeButton = ButtonState.ENABLED
        closeButtonLabel = CloseButtonLabel.LATER
        isImportProgressVisible = false
        isSettingsListEnabled = true
        statusText = StatusText.IMPORT_SUCCESS_PASSWORD_REQUIRED
    }

    fun showReadFailureText() {
        isLoadingProgressVisible = false
        isPickDocumentButtonVisible = true
        isPickDocumentButtonEnabled = true
        statusText = StatusText.IMPORT_READ_FAILURE
        importButton = ButtonState.DISABLED
    }

    fun showImportErrorText() {
        isLoadingProgressVisible = false
        isImportProgressVisible = false
        isSettingsListVisible = false
        isPickDocumentButtonVisible = true
        isPickDocumentButtonEnabled = true
        statusText = StatusText.IMPORT_FAILURE
        importButton = ButtonState.DISABLED
    }

    private fun showPartialImportErrorText() {
        importButton = ButtonState.GONE
        closeButton = ButtonState.ENABLED
        closeButtonLabel = CloseButtonLabel.OK
        isImportProgressVisible = false
        isSettingsListEnabled = true
        statusText = StatusText.IMPORT_PARTIAL_FAILURE
    }

}

sealed class SettingsListItem {
    var selected: Boolean = true
    var enabled: Boolean = true
    var importStatus: ImportStatus = ImportStatus.NOT_AVAILABLE

    class GeneralSettings : SettingsListItem()
    class Account(val accountIndex: Int, var displayName: String) : SettingsListItem()
}

enum class ImportStatus {
    NOT_AVAILABLE,
    NOT_SELECTED,
    IMPORT_SUCCESS,
    IMPORT_SUCCESS_PASSWORD_REQUIRED,
    IMPORT_FAILURE;

    val isSuccess: Boolean
        get() = this == IMPORT_SUCCESS || this == IMPORT_SUCCESS_PASSWORD_REQUIRED
}

enum class ButtonState {
    DISABLED,
    ENABLED,
    INVISIBLE,
    GONE
}

enum class StatusText {
    HIDDEN,
    IMPORTING_PROGRESS,
    IMPORT_SUCCESS,
    IMPORT_SUCCESS_PASSWORD_REQUIRED,
    IMPORT_READ_FAILURE,
    IMPORT_PARTIAL_FAILURE,
    IMPORT_FAILURE
}

enum class CloseButtonLabel {
    OK,
    LATER
}
