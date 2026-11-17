package com.woocommerce.android.ui.woopos.roles.pin

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
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
import java.security.ProviderException

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
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        store.setPIN(1, WooPosLocalRole.CASHIER, "5678").getOrThrow()
        store.setPIN(2, WooPosLocalRole.ADMIN, "0000").getOrThrow()
        val plaintext = requireNotNull(storage.read(1))
        assertThat(plaintext.toString(Charsets.UTF_8)).startsWith("{")
        val ciphertext = File(directory, "1.pins").readBytes()
        assertThat(ciphertext).isNotEqualTo(plaintext)
        assertThat(directory.canonicalPath).startsWith(context.noBackupFilesDir.canonicalPath)
        assertThat(directory.canonicalPath).doesNotStartWith(context.filesDir.canonicalPath)

        val recreatedStore = WooPosLocalPINStore(
            WooPosEncryptedPINStorage(context, "woopos.pin-test"), CoroutineDispatchers()
        )
        val recreated = WooPosPINAuthenticator(CoroutineDispatchers())
        val pins = recreatedStore.getPINs(1).getOrThrow()
        val otherStorePINs = recreatedStore.getPINs(2).getOrThrow()
        assertThat(recreated.authenticate(pins, "1234").getOrThrow()).isEqualTo(WooPosLocalRole.ADMIN)
        assertThat(recreated.authenticate(pins, "5678").getOrThrow()).isEqualTo(WooPosLocalRole.CASHIER)
        assertThat(recreated.authenticate(otherStorePINs, "1234").getOrThrow()).isNull()
        assertThat(recreated.authenticate(otherStorePINs, "0000").getOrThrow()).isNotNull()
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
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        val file = File(directory, "1.pins")
        file.writeBytes(file.readBytes().copyOf(WooPosPINStorage.MAX_RECORD_BYTES + 12 + 16 + 1))

        assertThatThrownBy { storage.read(1) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(store.getPINs(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
    }

    @Test
    fun ciphertextTamperingAndCrossStoreCopiesFailClosed(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        val original = File(directory, "1.pins").readBytes()
        File(directory, "2.pins").writeBytes(original)
        assertThat(store.getPINs(2).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        File(directory, "1.pins").writeBytes(original.apply { this[lastIndex] = (last().toInt() xor 1).toByte() })
        assertThat(store.getPINs(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
    }

    @Test
    fun missingKeystoreKeyDoesNotDisableProtectionOrReplaceCredentials(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry("woopos.pin-test")
        assertThat(store.getPINs(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        assertThat(store.setPIN(1, WooPosLocalRole.ADMIN, "0000").exceptionOrNull())
            .isEqualTo(WooPosPINError.StorageFailure)
    }

    @Test
    fun failedAtomicCommitDoesNotReportSuccessfulPINChange(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        File(directory, "1.pins.new").writeBytes(byteArrayOf(0))
        check(directory.setWritable(false))
        try {
            assertThat(store.setPIN(1, WooPosLocalRole.ADMIN, "5678").exceptionOrNull())
                .isEqualTo(WooPosPINError.StorageFailure)
            val authenticator = WooPosPINAuthenticator(CoroutineDispatchers())
            assertThat(authenticator.authenticate(store.getPINs(1).getOrThrow(), "1234").getOrThrow()).isNotNull()
            assertThat(authenticator.authenticate(store.getPINs(1).getOrThrow(), "5678").getOrThrow()).isNull()
        } finally {
            check(directory.setWritable(true))
        }
    }

    @Test
    fun inaccessibleDirectoryDoesNotReportSuccessfulAdminRemoval(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        check(directory.setExecutable(false))
        try {
            assertThat(store.removePIN(1, WooPosLocalRole.ADMIN).exceptionOrNull())
                .isEqualTo(WooPosPINError.StorageFailure)
            assertThat(store.getPINs(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        } finally {
            check(directory.setExecutable(true))
        }
        assertThat(
            WooPosPINAuthenticator(CoroutineDispatchers())
                .authenticate(store.getPINs(1).getOrThrow(), "1234").getOrThrow()
        )
            .isNotNull()
    }

    @Test
    fun keyErasureFailureStillDeletesRecords(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        val reported = mutableListOf<String>()
        val failingKeyStore = WooPosEncryptedPINStorage(
            context,
            "woopos.pin-test",
            reportFailure = { operation, _ -> reported += operation },
        ) { error("Keystore unavailable") }
        WooPosLocalPINStore(failingKeyStore, CoroutineDispatchers()).clearAll().getOrThrow()
        assertThat(store.getPINs(1).getOrThrow()).isEmpty()
        assertThat(directory.exists()).isFalse()
        assertThat(reported).containsExactly("deleteKey")
    }

    @Test
    fun fileErasureFailureStillInvalidatesKeyAndRemnantsFailClosed(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        check(directory.setWritable(false))
        try {
            store.clearAll().getOrThrow()
            assertThat(File(directory, "1.pins").exists()).isTrue()
            assertThat(store.getPINs(1).exceptionOrNull()).isEqualTo(WooPosPINError.StorageFailure)
        } finally {
            check(directory.setWritable(true))
        }
    }

    @Test
    fun failureOfBothErasureMethodsIsReportedWithoutClaimingRemoval(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        val failingKeyStore = WooPosEncryptedPINStorage(context, "woopos.pin-test") {
            error("Keystore unavailable")
        }
        check(directory.setWritable(false))
        try {
            assertThat(WooPosLocalPINStore(failingKeyStore, CoroutineDispatchers()).clearAll().exceptionOrNull())
                .isEqualTo(WooPosPINError.StorageFailure)
            assertThat(File(directory, "1.pins").exists()).isTrue()
            assertThat(store.getPINs(1).getOrThrow()).hasSize(1)
        } finally {
            check(directory.setWritable(true))
        }
    }

    @Test
    fun logoutErasesAllStoresAndAllowsFreshSetup(): Unit = runBlocking {
        val store = WooPosLocalPINStore(storage, CoroutineDispatchers())
        store.setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        store.setPIN(2, WooPosLocalRole.ADMIN, "5678").getOrThrow()
        store.clearAll().getOrThrow()
        assertThat(store.getPINs(1).getOrThrow()).isEmpty()
        assertThat(store.getPINs(2).getOrThrow()).isEmpty()
        assertThat(
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .containsAlias("woopos.pin-test")
        ).isFalse()
        store.setPIN(1, WooPosLocalRole.ADMIN, "0000").getOrThrow()
        assertThat(
            WooPosPINAuthenticator(CoroutineDispatchers())
                .authenticate(store.getPINs(1).getOrThrow(), "1234").getOrThrow()
        ).isNull()
    }

    @Test
    fun recordThatCannotBeDecryptedFailsSetupInsteadOfFirstUnlock(): Unit = runBlocking {
        var keyStoreCalls = 0
        val decryptFailing = WooPosEncryptedPINStorage(context, "woopos.pin-test") {
            check(keyStoreCalls++ == 0) { "Keystore unavailable" }
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        }
        val store = WooPosLocalPINStore(decryptFailing, CoroutineDispatchers())
        assertThat(store.setPIN(1, WooPosLocalRole.ADMIN, "1234").exceptionOrNull())
            .isEqualTo(WooPosPINError.StorageFailure)
        assertThat(WooPosLocalPINStore(storage, CoroutineDispatchers()).getPINs(1).getOrThrow()).isEmpty()
    }

    @Test
    fun storageFailureIsReportedWithItsOperation(): Unit = runBlocking {
        WooPosLocalPINStore(storage, CoroutineDispatchers()).setPIN(1, WooPosLocalRole.ADMIN, "1234").getOrThrow()
        val reported = mutableListOf<String>()
        val failing = WooPosEncryptedPINStorage(context, "woopos.pin-test", { operation, _ -> reported += operation }) {
            error("Keystore unavailable")
        }
        assertThat(WooPosLocalPINStore(failing, CoroutineDispatchers()).getPINs(1).exceptionOrNull())
            .isEqualTo(WooPosPINError.StorageFailure)
        assertThat(reported).containsExactly("read")
    }

    @Test
    fun failingReportDoesNotReplaceTheStorageFailure() {
        WooPosEncryptedPINStorage(context, "woopos.pin-test").write(1, byteArrayOf(1))
        val failing = WooPosEncryptedPINStorage(
            context,
            "woopos.pin-test",
            reportFailure = { _, _ -> error("Report failed") },
        ) { throw ProviderException("Keystore unavailable") }
        assertThatThrownBy { failing.read(1) }.isInstanceOf(ProviderException::class.java)
    }
}
