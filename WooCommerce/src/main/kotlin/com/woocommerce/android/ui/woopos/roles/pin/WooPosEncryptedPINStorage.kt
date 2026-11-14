package com.woocommerce.android.ui.woopos.roles.pin

import android.content.Context
import android.os.Build
import android.security.KeyStoreException
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
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

    private val directory by lazy { File(context.noBackupFilesDir, "woopos-pins") }

    @Synchronized
    override fun read(localSiteId: Int): ByteArray? = reporting("read") { readRecord(localSiteId) }

    @Synchronized
    override fun write(localSiteId: Int, bytes: ByteArray?) = reporting("write") { writeRecord(localSiteId, bytes) }

    @Synchronized
    override fun clearAll() = reporting("clearAll") {
        if (!directory.exists()) return@reporting
        // Erase the key first: even interrupted file cleanup must not leave usable PIN records.
        val keyErased = runCatching { retryingTransientFailure { keyStoreProvider().deleteEntry(keyAlias) } }.isSuccess
        val filesErased = runCatching { directory.deleteRecursively() }.getOrDefault(false)
        check(keyErased || filesErased)
    }

    private fun readRecord(localSiteId: Int): ByteArray? {
        checkDirectoryAccess()
        val file = file(localSiteId)
        if (!exists(file)) return null
        val bytes = file.openRead().use { input -> input.readBytesBounded() }
        require(bytes.size > IV_BYTES + TAG_BYTES)
        return retryingTransientFailure {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                requireNotNull(keyStoreProvider().getKey(keyAlias, null)),
                GCMParameterSpec(TAG_BYTES * Byte.SIZE_BITS, bytes.copyOfRange(0, IV_BYTES))
            )
            cipher.updateAAD(localSiteId.toString().toByteArray(Charsets.UTF_8))
            cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES)
        }
    }

    private fun writeRecord(localSiteId: Int, bytes: ByteArray?) {
        val file = file(localSiteId)
        if (bytes == null) {
            checkDirectoryAccess()
            file.delete()
            check(!exists(file))
            return
        }
        require(bytes.size <= WooPosPINStorage.MAX_RECORD_BYTES)
        check(directory.isDirectory || directory.mkdirs())
        val encrypted = retryingTransientFailure {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(localSiteId.toString().toByteArray(Charsets.UTF_8))
            cipher.iv + cipher.doFinal(bytes)
        }
        val stream = file.startWrite()
        try {
            stream.write(encrypted)
            stream.fd.sync()
            file.finishWrite(stream)
        } catch (e: IOException) {
            file.failWrite(stream)
            throw e
        }
        // Decrypting once catches devices that can encrypt but not decrypt before the PIN is relied on.
        check(readRecord(localSiteId)?.contentEquals(bytes) == true)
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> reporting(operation: String, block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            reportFailure(operation, e)
            throw e
        }

    private fun checkDirectoryAccess() {
        check(!directory.exists() || (directory.isDirectory && directory.canRead() && directory.canExecute()))
    }

    private fun exists(file: AtomicFile): Boolean =
        file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()

    private fun file(localSiteId: Int): AtomicFile {
        require(localSiteId > 0)
        return AtomicFile(File(directory, "$localSiteId.pins"))
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
        private const val MAX_REPORTED_CAUSES = 5

        // KeyStoreException.isTransientFailure() is public from API 33; older versions can't tell, so retry once.
        internal fun isTransientFailure(failure: Throwable): Boolean = when {
            failure is AEADBadTagException -> false
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

        private fun causes(failure: Throwable) = generateSequence(failure) { it.cause }.take(MAX_REPORTED_CAUSES)
    }
}
