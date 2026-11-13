package com.woocommerce.android.ui.woopos.roles

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.security.KeyStore
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
    private val keyStoreProvider: () -> KeyStore = { KeyStore.getInstance(KEYSTORE).apply { load(null) } },
) : WooPosPINStorage {
    @Inject constructor(@ApplicationContext context: Context) : this(context, KEY_ALIAS)

    private val directory = File(context.noBackupFilesDir, "woopos-pins")

    @Synchronized
    override fun read(localSiteId: Int): ByteArray? {
        checkDirectoryAccess()
        val file = file(localSiteId)
        if (!exists(file)) return null
        val bytes = file.openRead().use { input -> input.readBytesBounded() }
        require(bytes.size > IV_BYTES + TAG_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            requireNotNull(keyStore().getKey(keyAlias, null)),
            GCMParameterSpec(TAG_BYTES * Byte.SIZE_BITS, bytes.copyOfRange(0, IV_BYTES))
        )
        cipher.updateAAD(localSiteId.toString().toByteArray(Charsets.UTF_8))
        return cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES)
    }

    @Synchronized
    override fun write(localSiteId: Int, bytes: ByteArray?) {
        val file = file(localSiteId)
        if (bytes == null) {
            checkDirectoryAccess()
            file.delete()
            check(!exists(file))
            return
        }
        require(bytes.size <= WooPosPINStorage.MAX_RECORD_BYTES)
        check(directory.isDirectory || directory.mkdirs())
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(localSiteId.toString().toByteArray(Charsets.UTF_8))
        val encrypted = cipher.iv + cipher.doFinal(bytes)
        val stream = file.startWrite()
        try {
            stream.write(encrypted)
            stream.fd.sync()
            file.finishWrite(stream)
        } catch (e: IOException) {
            file.failWrite(stream)
            throw e
        }
        check(file.openRead().use { it.readBytesBounded() }.contentEquals(encrypted))
    }

    @Synchronized
    override fun clearAll() {
        if (!directory.exists()) return
        // Erase the key first: even interrupted file cleanup must not leave usable PIN records.
        val keyErased = runCatching { keyStore().deleteEntry(keyAlias) }.isSuccess
        val filesErased = runCatching { directory.deleteRecursively() }.getOrDefault(false)
        check(keyErased || filesErased)
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

    private fun keyStore(): KeyStore = keyStoreProvider()

    private fun getOrCreateKey(): SecretKey {
        val store = keyStore()
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

    private fun java.io.InputStream.readBytesBounded(): ByteArray {
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

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "woopos.local-pins.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val KEY_BITS = 256
    }
}
