package com.fsck.k9.ui.crypto.keyimport

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val secretKeyImportModule = module {
    viewModel { SecretKeyImportViewModel(get(), get()) }
}
