package com.woocommerce.android.ui.woopos.roles

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class WooPosEncryptedPINStorageTest {
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getNoBackupFilesDir(): File = File(super.getNoBackupFilesDir(), "pin-test").apply { mkdirs() }
    }
    private val storage = WooPosEncryptedPINStorage(context, "woopos.pin-test")
    private val directory get() = File(context.noBackupFilesDir, "woopos-pins")

    @Before
    fun setUp() = storage.clearAll()

    @After
    fun tearDown() {
        storage.clearAll()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry("woopos.pin-test")
    }

    @Test
    fun encryptedRecordsSurviveRecreationAndRemainStoreScoped(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        store.setPIN(1, WooPosRole.CASHIER, "5678").getOrThrow()
        store.setPIN(2, WooPosRole.ADMIN, "0000").getOrThrow()
        val plaintext = requireNotNull(storage.read(1))
        assertThat(plaintext.toString(Charsets.UTF_8)).startsWith("{")
        val ciphertext = File(directory, "1.pins").readBytes()
        assertThat(ciphertext).isNotEqualTo(plaintext)
        assertThat(directory.canonicalPath).startsWith(context.noBackupFilesDir.canonicalPath)
        assertThat(directory.canonicalPath).doesNotStartWith(context.filesDir.canonicalPath)

        val recreated = WooPosPINAuthenticator(
            WooPosLocalPINStore(WooPosEncryptedPINStorage(context, "woopos.pin-test"), CoroutineDispatchers()),
            CoroutineDispatchers()
        )
        assertThat(recreated.authenticate(1, "1234").getOrThrow()?.allows(WooPosPermission.EXIT_POS)).isTrue()
        assertThat(recreated.authenticate(1, "5678").getOrThrow()?.allows(WooPosPermission.EXIT_POS)).isFalse()
        assertThat(recreated.authenticate(2, "1234").getOrThrow()).isNull()
        assertThat(recreated.authenticate(2, "0000").getOrThrow()).isNotNull()
    }

    @Test
    fun legacyRecordsRemainReadableAndChangeToJsonOnlyAfterMutation(): Unit = runBlocking {
        val legacy = LEGACY_RECORD.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        storage.write(1, legacy)
        val ciphertext = File(directory, "1.pins").readBytes()
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        val authenticator = WooPosPINAuthenticator(store, CoroutineDispatchers())

        assertThat(store.credentials(1).getOrThrow()).hasSize(2)
        assertThat(authenticator.authenticate(1, "1234").getOrThrow()?.allows(WooPosPermission.EXIT_POS)).isTrue()
        assertThat(authenticator.authenticate(1, "5678").getOrThrow()?.allows(WooPosPermission.EXIT_POS)).isFalse()
        assertThat(storage.read(1)).isEqualTo(legacy)
        assertThat(File(directory, "1.pins").readBytes()).isEqualTo(ciphertext)

        store.setPIN(1, WooPosRole.ADMIN, "0000").getOrThrow()
        assertThat(requireNotNull(storage.read(1)).toString(Charsets.UTF_8)).startsWith("{")
        val recreated = WooPosPINAuthenticator(
            WooPosLocalPINStore(WooPosEncryptedPINStorage(context, "woopos.pin-test"), CoroutineDispatchers()),
            CoroutineDispatchers()
        )
        assertThat(recreated.authenticate(1, "0000").getOrThrow()?.allows(WooPosPermission.EXIT_POS)).isTrue()
        assertThat(recreated.authenticate(1, "5678").getOrThrow()?.allows(WooPosPermission.EXIT_POS)).isFalse()
        assertThat(recreated.authenticate(1, "1234").getOrThrow()).isNull()
    }

    @Test
    fun maximumSizeRecordSurvivesRecreationAndOversizedWritePreservesIt() {
        val bytes = ByteArray(WooPosPINStorage.MAX_RECORD_BYTES) { it.toByte() }
        storage.write(1, bytes)
        val recreated = WooPosEncryptedPINStorage(context, "woopos.pin-test")
        assertThat(recreated.read(1)).isEqualTo(bytes)
        val ciphertext = File(directory, "1.pins").readBytes()

        assertThatThrownBy { storage.write(1, bytes + byteArrayOf(0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(File(directory, "1.pins").readBytes()).isEqualTo(ciphertext)
        assertThat(recreated.read(1)).isEqualTo(bytes)
    }

    @Test
    fun oversizedEncryptedRecordIsRejectedAsStorageFailure(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        val file = File(directory, "1.pins")
        file.writeBytes(file.readBytes().copyOf(WooPosPINStorage.MAX_RECORD_BYTES + 12 + 16 + 1))

        assertThatThrownBy { storage.read(1) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(store.credentials(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
    }

    @Test
    fun ciphertextTamperingAndCrossStoreCopiesFailClosed(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        val original = File(directory, "1.pins").readBytes()
        File(directory, "2.pins").writeBytes(original)
        assertThat(store.credentials(2).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        File(directory, "1.pins").writeBytes(original.apply { this[lastIndex] = (last().toInt() xor 1).toByte() })
        assertThat(store.credentials(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
    }

    @Test
    fun missingKeystoreKeyDoesNotDisableProtectionOrReplaceCredentials(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry("woopos.pin-test")
        assertThat(store.credentials(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        assertThat(store.setPIN(1, WooPosRole.ADMIN, "0000").exceptionOrNull())
            .isEqualTo(WooPosPINError.StorageFailure)
    }

    @Test
    fun failedAtomicCommitDoesNotReportSuccessfulPINChange(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        File(directory, "1.pins.new").writeBytes(byteArrayOf(0))
        check(directory.setWritable(false))
        try {
            assertThat(store.setPIN(1, WooPosRole.ADMIN, "5678").exceptionOrNull())
                .isEqualTo(WooPosPINError.StorageFailure)
            val authenticator = WooPosPINAuthenticator(store, CoroutineDispatchers())
            assertThat(authenticator.authenticate(1, "1234").getOrThrow()).isNotNull()
            assertThat(authenticator.authenticate(1, "5678").getOrThrow()).isNull()
        } finally {
            check(directory.setWritable(true))
        }
    }

    @Test
    fun inaccessibleDirectoryDoesNotReportSuccessfulAdminRemoval(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        check(directory.setExecutable(false))
        try {
            assertThat(store.removePIN(1, WooPosRole.ADMIN).exceptionOrNull())
                .isEqualTo(WooPosPINError.StorageFailure)
            assertThat(store.credentials(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        } finally {
            check(directory.setExecutable(true))
        }
        assertThat(WooPosPINAuthenticator(store, CoroutineDispatchers()).authenticate(1, "1234").getOrThrow())
            .isNotNull()
    }

    @Test
    fun keyErasureFailureStillDeletesRecords(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        val failingKeyStore = WooPosEncryptedPINStorage(context, "woopos.pin-test") {
            error("Keystore unavailable")
        }
        WooPosLocalPINStore(failingKeyStore, CoroutineDispatchers()).clearAll().getOrThrow()
        assertThat(store.credentials(1).getOrThrow()).isEmpty()
        assertThat(directory.exists()).isFalse()
    }

    @Test
    fun fileErasureFailureStillInvalidatesKeyAndRemnantsFailClosed(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        check(directory.setWritable(false))
        try {
            store.clearAll().getOrThrow()
            assertThat(File(directory, "1.pins").exists()).isTrue()
            assertThat(store.credentials(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        } finally {
            check(directory.setWritable(true))
        }
    }

    @Test
    fun failureOfBothErasureMethodsIsReportedWithoutClaimingRemoval(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        val failingKeyStore = WooPosEncryptedPINStorage(context, "woopos.pin-test") {
            error("Keystore unavailable")
        }
        check(directory.setWritable(false))
        try {
            assertThat(WooPosLocalPINStore(failingKeyStore, CoroutineDispatchers()).clearAll().exceptionOrNull())
                .isEqualTo(WooPosPINError.StorageFailure)
            assertThat(File(directory, "1.pins").exists()).isTrue()
            assertThat(store.credentials(1).getOrThrow()).hasSize(1)
        } finally {
            check(directory.setWritable(true))
        }
    }

    @Test
    fun logoutErasesAllStoresAndAllowsFreshSetup(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosRole.ADMIN, "1234").getOrThrow()
        store.setPIN(2, WooPosRole.ADMIN, "5678").getOrThrow()
        store.clearAll().getOrThrow()
        assertThat(store.credentials(1).getOrThrow()).isEmpty()
        assertThat(store.credentials(2).getOrThrow()).isEmpty()
        assertThat(
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .containsAlias("woopos.pin-test")
        ).isFalse()
        store.setPIN(1, WooPosRole.ADMIN, "0000").getOrThrow()
        assertThat(WooPosPINAuthenticator(store, CoroutineDispatchers()).authenticate(1, "1234").getOrThrow()).isNull()
    }

    companion object {
        private const val LEGACY_RECORD =
            "000000010000000201000186a0000102030405060708090a0b0c0d0e0f" +
                "869e6c8350c5beb0acc399fbaac3b60d220433896b26a647734d0d8f1586e1fa" +
                "02000186a0101112131415161718191a1b1c1d1e1f" +
                "5c1a9859e3442fbf88975a1f0879ededf84e4298da7d98654d4a5f92fa827566"
    }
}
