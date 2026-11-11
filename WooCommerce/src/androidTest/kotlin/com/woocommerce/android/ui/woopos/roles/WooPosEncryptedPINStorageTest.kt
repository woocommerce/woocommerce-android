package com.woocommerce.android.ui.woopos.roles

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
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
}
