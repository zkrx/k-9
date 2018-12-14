package com.fsck.k9.ui.settings.imports

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.preferences.SettingsImportExportException
import com.fsck.k9.preferences.SettingsImporter
import com.fsck.k9.ui.R
import com.fsck.k9.ui.observe
import kotlinx.android.synthetic.main.activity_import.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import timber.log.Timber
import java.io.FileNotFoundException

class ImportSettingsActivity : K9Activity() {
    private lateinit var viewModel: ImportViewModel
    private lateinit var adapter: ImportSettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_import)

        viewModel = ViewModelProviders.of(this).get(ImportViewModel::class.java)
        if (!viewModel.initialized) {
            initialize()
        }
        viewModel.importContentsLiveData.observe(this, this::onImportContentLoaded)

        button_import_import.setOnClickListener { onClickImport() }
        this.adapter = ImportSettingsAdapter(layoutInflater)
        recycler_import_accounts.adapter = adapter
        recycler_import_accounts.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun initialize() {
        viewModel.initialized = true
        viewModel.importContentsLiveData = ImportContentsLiveData(contentResolver)
        val openDocumentIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        text_import_info.visibility = View.INVISIBLE
        checkbox_import_general.visibility = View.INVISIBLE
        button_import_import.visibility = View.INVISIBLE
        recycler_import_accounts.visibility = View.INVISIBLE
        progress_import.visibility = View.VISIBLE

        startActivityForResult(openDocumentIntent, REQUEST_CODE_OPEN)
    }

    class ImportViewModel : ViewModel() {
        internal lateinit var importContentsLiveData: ImportContentsLiveData
        internal var uri: Uri? = null
        internal var initialized = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode != REQUEST_CODE_OPEN || resultCode != RESULT_OK || resultData == null || resultData.data == null) {
            finish()
            return
        }

        viewModel.uri = resultData.data
        viewModel.importContentsLiveData.loadImportDataAsync(resultData.data)
    }

    private fun onImportContentLoaded(importContents: SettingsImporter.ImportContents?) {
        if (importContents == null) {
            finish()
            return
        }

        checkbox_import_general.isEnabled = importContents.globalSettings
        adapter.replaceData(importContents.accounts)

        launch(UI) {
            delay(UX_DELAY_MEDIUM)

            TransitionManager.beginDelayedTransition(content)
            text_import_info.visibility = View.VISIBLE
            checkbox_import_general.visibility = View.VISIBLE
            button_import_import.visibility = View.VISIBLE
            recycler_import_accounts.visibility = View.VISIBLE
            progress_import.visibility = View.GONE
        }
    }

    private fun onClickImport() {
        val importGeneralSettings = checkbox_import_general.isChecked
        val accountUuidsToImport = adapter.getCheckedUuids()

        launch(UI) {
            bg {
                importSettings(importGeneralSettings, accountUuidsToImport)
            }.await()

            finish()
        }
    }

    private fun importSettings(importGeneralSettings: Boolean, accountUuidsToImport: List<String>) {
        contentResolver.openInputStream(viewModel.uri).use {
            SettingsImporter.importSettings(this, it, importGeneralSettings, accountUuidsToImport, true)
        }
    }

    class ImportContentsLiveData(
            private val contentResolver: ContentResolver
    ) : LiveData<SettingsImporter.ImportContents?>() {
        fun loadImportDataAsync(uri: Uri) {
            launch(UI) {
                val data = bg {
                    try {
                        contentResolver.openInputStream(uri).use {
                            SettingsImporter.getImportStreamContents(it)
                        }
                    } catch (e: SettingsImportExportException) {
                        Timber.w(e, "Exception during export")
                        null
                    } catch (e: FileNotFoundException) {
                        Timber.w("Couldn't read content from URI %s", uri)
                        null
                    }
                }

                value = data.await()
            }
        }
    }

    companion object {
        const val REQUEST_CODE_OPEN = 1
        const val UX_DELAY_MEDIUM = 600L

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, ImportSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

}