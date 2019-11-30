package com.fsck.k9.ui.crypto.keyimport

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fsck.k9.crypto.openpgp.SecretKeyImporter
import com.fsck.k9.crypto.openpgp.SecretKeyInfo
import com.fsck.k9.helper.SingleLiveEvent
import com.fsck.k9.helper.measureRealtimeMillisWithResult
import com.fsck.k9.ui.helper.CoroutineScopeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SecretKeyImportViewModel(
    private val context: Context,
    private val secretKeyImporter: SecretKeyImporter
) : CoroutineScopeViewModel() {
    private val uiModelLiveData = MutableLiveData<SecretKeyImportUiState>()
    private val actionLiveData = SingleLiveEvent<Action>()

    private val uiState = SecretKeyImportUiState()
    private var contentUri: Uri? = null

    fun getActionEvents(): LiveData<Action> = actionLiveData

    fun getUiState(): LiveData<SecretKeyImportUiState> {
        if (uiModelLiveData.value == null) {
            uiModelLiveData.value = uiState
        }

        return uiModelLiveData
    }

    fun initializeFromSavedState(savedInstanceState: Bundle) {
        //TODO: implement
    }

    fun saveInstanceState(outState: Bundle) {
        //TODO: implement
    }

    fun onPickDocumentButtonClicked() {
        updateUiState {
            disablePickDocumentButton()
        }

        sendActionEvent(Action.PickDocument)
    }

    fun onDocumentPickCanceled() {
        updateUiState {
            enablePickDocumentButton()
        }
    }

    fun onDocumentPicked(contentUri: Uri) {
        updateUiState {
            showLoadingProgress()
        }

        startReadSecretKeyFile(contentUri)
    }

    fun onImportButtonClicked() {
        updateUiState {
            showImportingProgress()
        }

        startImportSettings()
    }

    fun onCloseButtonClicked() {
        sendActionEvent(Action.Close)
    }

    fun onSettingsListItemClicked(position: Int) {
        //TODO: implement
    }

    private fun startReadSecretKeyFile(contentUri: Uri) {
        this.contentUri = contentUri

        launch {
            try {
                val (elapsed, contents) = measureRealtimeMillisWithResult {
                    withContext(Dispatchers.IO) {
                        readSecretKeyRings(contentUri)
                    }
                }

                if (elapsed < MIN_PROGRESS_DURATION) {
                    delay(MIN_PROGRESS_DURATION - elapsed)
                }

                updateUiState {
                    settingsList = contents
                    isSettingsListVisible = true
                    isSettingsListEnabled = true
                    isLoadingProgressVisible = false
                }

            } catch (e: Exception) {
                Timber.e(e, "Error reading settings file")

                updateUiState {
                    showReadFailureText()
                }
            }
        }
    }

    private fun startImportSettings() {
        val contentUri = this.contentUri ?: error("contentUri is missing")
        launch {
            try {
                //TODO: implement
            } catch (e: Exception) {
                Timber.e(e, "Error importing settings")

                updateUiState {
                    showImportErrorText()
                }
            }
        }
    }

    private fun readSecretKeyRings(contentUri: Uri): List<SecretKeyInfo> {
        return context.contentResolver.openInputStream(contentUri)?.use { inputStream ->
            secretKeyImporter.findSecretKeyRings(inputStream)
        } ?: error("Couldn't open file")
    }

    private fun updateUiState(block: SecretKeyImportUiState.() -> Unit) {
        uiState.block()
        uiModelLiveData.value = uiState
    }

    private fun sendActionEvent(action: Action) {
        actionLiveData.value = action
    }

    companion object {
        private const val MIN_PROGRESS_DURATION = 500L
    }
}

sealed class Action {
    object Close : Action()
    object PickDocument : Action()
}
