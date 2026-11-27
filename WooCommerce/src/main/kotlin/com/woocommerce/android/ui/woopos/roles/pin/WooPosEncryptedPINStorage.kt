package com.woocommerce.android.ui.woopos.roles.pin

import android.content.Context
import android.os.Build
import android.security.KeyStoreException
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosEncryptedPINStorage internal constructor(
    context: Context,
    private val keyAlias: String,
    private val reportFailure: (operation: String, failure: Throwable) -> Unit = { _, _ -> },
    private val keyStoreProvider: () -> KeyStore = { KeyStore.getInstance(KEYSTORE).apply { load(null) } },
) : WooPosPINStorage {
    @Inject constructor(@ApplicationContext context: Context, crashLogging: CrashLogging) : this(
        context,
        KEY_ALIAS,
        reportFailure = { operation, failure ->
            crashLogging.sendReport(
                exception = sanitizedFailure(operation, failure),
                tags = mapOf("operation" to operation),
            )
        },
    )

    private val files by lazy { WooPosPINStorageFiles(File(context.noBackupFilesDir, "woopos-pins"), "pins") }
    private val directory get() = files.directory

    @Synchronized
    override fun read(localSiteId: Int): ByteArray? = reporting("read") { readRecord(localSiteId) }

    @Synchronized
    override fun write(localSiteId: Int, bytes: ByteArray?, canCommit: () -> Boolean): Boolean =
        reporting("write") { writeRecord(localSiteId, bytes, canCommit) }

    @Synchronized
    override fun clearAll() = reporting("clearAll") {
        if (!directory.exists()) return@reporting
        // Erase the key first: even interrupted file cleanup must not leave usable PIN records.
        val keyErasure = runCatching { retryingTransientFailure { keyStoreProvider().deleteEntry(keyAlias) } }
        keyErasure.exceptionOrNull()?.let { report("deleteKey", it) }
        val keyErased = keyErasure.isSuccess
        val filesErased = runCatching { directory.deleteRecursively() }.getOrDefault(false)
        check(keyErased || filesErased)
    }

    private fun readRecord(localSiteId: Int): ByteArray? {
        files.checkDirectoryAccess()
        val file = files.file(localSiteId)
        if (!files.exists(file)) return null
        return decrypt(localSiteId, file.openRead().use { input -> input.readBytesBounded() })
    }

    private fun decrypt(localSiteId: Int, encrypted: ByteArray): ByteArray {
        require(encrypted.size > IV_BYTES + TAG_BYTES)
        return retryingTransientFailure {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                requireNotNull(keyStoreProvider().getKey(keyAlias, null)),
                GCMParameterSpec(TAG_BYTES * Byte.SIZE_BITS, encrypted.copyOfRange(0, IV_BYTES))
            )
            cipher.updateAAD(localSiteId.toString().toByteArray(Charsets.UTF_8))
            cipher.doFinal(encrypted, IV_BYTES, encrypted.size - IV_BYTES)
        }
    }

    private fun writeRecord(localSiteId: Int, bytes: ByteArray?, canCommit: () -> Boolean): Boolean {
        files.checkDirectoryAccess()
        val record = files.file(localSiteId).baseFile
        val backup = File(record.path + ".bak")
        if (backup.exists()) Files.move(backup.toPath(), record.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        val staged = File(record.path + ".new")
        if (bytes == null) {
            Files.deleteIfExists(staged.toPath())
            if (!canCommit()) return false
            Files.deleteIfExists(record.toPath())
            return true
        }
        require(bytes.size <= WooPosPINStorage.MAX_RECORD_BYTES)
        check(directory.isDirectory || directory.mkdirs())
        val encrypted = retryingTransientFailure {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(localSiteId.toString().toByteArray(Charsets.UTF_8))
            cipher.iv + cipher.doFinal(bytes)
        }
        var committed = false
        try {
            staged.outputStream().use {
                it.write(encrypted)
                it.fd.sync()
            }
            val stagedCiphertext = staged.inputStream().use { it.readBytesBounded() }
            check(decrypt(localSiteId, stagedCiphertext).contentEquals(bytes))
            if (!canCommit()) return false
            Files.move(staged.toPath(), record.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            committed = true
            return true
        } finally {
            if (!committed) staged.delete()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> reporting(operation: String, block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            report(operation, e)
            throw e
        }

    private fun report(operation: String, failure: Throwable) {
        runCatching { reportFailure(operation, failure) }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = keyStoreProvider()
        if (store.containsAlias(keyAlias)) return store.getKey(keyAlias, null) as SecretKey
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .build()
            )
        }.generateKey()
    }

    private fun InputStream.readBytesBounded(): ByteArray {
        val bytes = ByteArray(WooPosPINStorage.MAX_RECORD_BYTES + IV_BYTES + TAG_BYTES + 1)
        var count = 0
        while (count < bytes.size) {
            val read = read(bytes, count, bytes.size - count)
            if (read == -1) break
            count += read
        }
        require(count < bytes.size)
        return bytes.copyOf(count)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> retryingTransientFailure(block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            if (!isTransientFailure(e)) throw e
            Thread.sleep(RETRY_DELAY_MS)
            block()
        }

    private class PINStorageFailure(message: String) : Exception(message)

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "woopos.local-pins.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val KEY_BITS = 256
        private const val RETRY_DELAY_MS = 100L
        private const val MAX_CAUSE_DEPTH = 5

        // KeyStoreException.isTransientFailure() is public from API 33; older versions can't tell, so retry once.
        internal fun isTransientFailure(failure: Throwable): Boolean = when {
            failure is AEADBadTagException || failure is IllegalArgumentException || failure is IllegalStateException ->
                false
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                causes(failure).any { it is KeyStoreException && it.isTransientFailure }
            else -> true
        }

        // Exception messages can include record contents, so only class names and the Keystore code are reported.
        internal fun sanitizedFailure(operation: String, failure: Throwable): Throwable {
            val causes = causes(failure).toList()
            val keystoreCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                causes.filterIsInstance<KeyStoreException>().firstOrNull()?.numericErrorCode
            } else {
                null
            }
            val summary = causes.joinToString(" <- ") { it.javaClass.name } +
                keystoreCode?.let { " (Keystore error $it)" }.orEmpty()
            return PINStorageFailure("Local PIN storage $operation failed: $summary").apply {
                stackTrace = failure.stackTrace
            }
        }

        private fun causes(failure: Throwable) = generateSequence(failure) { it.cause }.take(MAX_CAUSE_DEPTH)
    }
}
