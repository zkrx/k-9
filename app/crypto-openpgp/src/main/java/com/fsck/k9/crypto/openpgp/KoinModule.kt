package com.fsck.k9.crypto.openpgp

import org.koin.dsl.module

val openPgpCryptoModule = module {
    factory { SecretKeyImporter() }
}
