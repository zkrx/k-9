package com.fsck.k9.crypto.openpgp

import java.io.InputStream
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType.DIVERT_TO_CARD
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType.GNU_DUMMY
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType.PASSPHRASE
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType.PASSPHRASE_EMPTY
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType.UNAVAILABLE
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow

class SecretKeyImporter {
    fun findSecretKeyRings(inputStream: InputStream): List<SecretKeyInfo> {
        val operationLog = OperationLog()

        return inputStream.use { stream ->
            val keyRingIterator = UncachedKeyRing.fromStream(stream)
            keyRingIterator.asIterator().asSequence()
                .mapNotNull { it.canonicalize(operationLog, 0) }
                .filterIsInstance<CanonicalizedSecretKeyRing>()
                .mapNotNull { it.toSecretKeyInfoOrNull() }
                .toList()
        }
    }

    private fun CanonicalizedSecretKeyRing.toSecretKeyInfoOrNull(): SecretKeyInfo? {
        val needsPassword = when (secretKey.secretKeyTypeSuperExpensive) {
            UNAVAILABLE -> return null
            GNU_DUMMY -> return null
            DIVERT_TO_CARD -> return null
            PASSPHRASE -> true
            PASSPHRASE_EMPTY -> false
        }

        return SecretKeyInfo(secretKeyRing = this, needsPassword = needsPassword)
    }

    private fun <T> IteratorWithIOThrow<T>.asIterator(): Iterator<T> {
        return ProperIterator(this)
    }

    private class ProperIterator<T>(private val iterator: IteratorWithIOThrow<T>) : Iterator<T> {
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): T = iterator.next()
    }
}

data class SecretKeyInfo(val secretKeyRing: CanonicalizedSecretKeyRing, val needsPassword: Boolean)
