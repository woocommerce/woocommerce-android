package com.woocommerce.android.ui.woopos.roles.lock

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.security.keystore.KeyInfo
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosEncryptedPINStorage
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINAuthenticator
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import java.io.File
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WooPosPINIntegrationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var siteStore: SiteStore

    @Inject
    lateinit var flags: FeatureFlagRepository

    @Inject
    @AppCoroutineScope
    lateinit var appScope: CoroutineScope

    private val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(baseContext.noBackupFilesDir, "pin-integration-test")
    private val context = object : ContextWrapper(baseContext) {
        override fun getApplicationContext(): Context = this
        override fun getNoBackupFilesDir(): File = directory.apply { mkdirs() }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences(PREFERENCES, mode)
    }
    private val dispatchers = CoroutineDispatchers()
    private val encrypted = WooPosEncryptedPINStorage(context, KEY_ALIAS)
    private val attempts = WooPosFilePINAttemptStorage(context)
    private lateinit var selectedSite: SelectedSite
    private lateinit var store: WooPosLocalPINStore

    @Before
    fun setUp(): Unit = runBlocking {
        AppPrefs.init(context)
        hiltRule.inject()
        flags.setFlagOverride(FeatureFlag.WOO_POS_ROLES, true)
        selectedSite = SelectedSite(context, siteStore).apply { set(SiteModel().apply { id = SITE_ID }) }
        encrypted.clearAll()
        attempts.clearAll()
        store = WooPosLocalPINStore(encrypted, dispatchers)
        store.setPIN(SITE_ID, WooPosLocalRole.ADMIN, ADMIN_PIN).getOrThrow()
        store.setPIN(SITE_ID, WooPosLocalRole.CASHIER, CASHIER_PIN).getOrThrow()
        reportKeySecurity()
    }

    @After
    fun tearDown() {
        encrypted.clearAll()
        attempts.clearAll()
        appScope.cancel()
        check(directory.deleteRecursively())
        check(baseContext.deleteSharedPreferences(PREFERENCES))
        AppPrefs.init(baseContext)
    }

    @Test
    fun savedPINsUnlockRecreatedSessionAndOnlyAdminResetsFailures(): Unit = runBlocking(Dispatchers.Main) {
        // GIVEN
        val recreatedStore = WooPosLocalPINStore(WooPosEncryptedPINStorage(context, KEY_ALIAS), dispatchers)
        val session = createSession(createLimiter(recreatedStore))
        session.initialize()
        assertThat(session.state.value.isLocked).isTrue()

        // WHEN
        enter(session, WRONG_PIN)
        val previous = attempts.read(SITE_ID)

        // THEN
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.INCORRECT)
        assertThat(session.activeRole).isNull()
        assertThat(previous.failures).isEqualTo(1)
        enter(session, CASHIER_PIN)
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.activeRole).isEqualTo(WooPosLocalRole.CASHIER)
        assertThat(attempts.read(SITE_ID)).isEqualTo(previous)
        session.lock()
        assertThat(session.activeRole).isNull()
        enter(session, ADMIN_PIN)
        assertThat(session.state.value.isLocked).isFalse()
        assertThat(session.activeRole).isEqualTo(WooPosLocalRole.ADMIN)
        assertThat(attempts.read(SITE_ID)).isEqualTo(WooPosPINAttemptState())
    }

    @Test
    fun sharedWaitSurvivesRecreationAndExpiresUsingAndroidClock(): Unit = runBlocking(Dispatchers.Main) {
        // GIVEN
        val limiter = createLimiter()
        val session = createSession(limiter)
        session.initialize()
        enter(session, CASHIER_PIN)
        repeat(5) {
            assertThat(limiter.authenticate(SITE_ID, CASHIER_PIN, WooPosCapability.ISSUE_REFUNDS))
                .isInstanceOf(WooPosLocalRateLimiterResult.Incorrect::class.java)
        }
        val previous = attempts.read(SITE_ID)
        assertThat(previous.failures).isEqualTo(5)
        assertThat(previous.waitMillis).isEqualTo(30_000)

        // WHEN
        session.lock()

        // THEN
        assertThat(session.state.value.remainingSeconds).isBetween(1, 30)
        assertThat(session.state.value.canEnterPIN).isFalse()
        assertThat(session.activeRole).isNull()
        enter(session, ADMIN_PIN)
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(attempts.read(SITE_ID)).isEqualTo(previous)

        val recreatedStore = WooPosLocalPINStore(WooPosEncryptedPINStorage(context, KEY_ALIAS), dispatchers)
        val recreated = createSession(createLimiter(recreatedStore))
        recreated.initialize()
        assertThat(recreated.state.value.remainingSeconds).isBetween(1, session.state.value.remainingSeconds)
        assertThat(recreated.state.value.canEnterPIN).isFalse()
        withTimeout(35_000) {
            while (recreated.state.value.remainingSeconds > 0) {
                delay(1000)
                recreated.refreshWait()
            }
        }
        assertThat(recreated.state.value.canEnterPIN).isTrue()
        enter(recreated, CASHIER_PIN)
        assertThat(recreated.state.value.isLocked).isFalse()
        assertThat(attempts.read(SITE_ID)).isEqualTo(previous)
        recreated.lock()
        enter(recreated, ADMIN_PIN)
        assertThat(recreated.state.value.isLocked).isFalse()
        assertThat(attempts.read(SITE_ID)).isEqualTo(WooPosPINAttemptState())
    }

    @Test
    fun corruptAttemptsBlockCorrectPINWithoutChangingPINsOrRepairingRecord(): Unit = runBlocking(Dispatchers.Main) {
        // GIVEN
        createLimiter().authenticate(SITE_ID, WRONG_PIN)
        val file = File(directory, "woopos-pin-attempts/$SITE_ID.attempts")
        val corrupt = file.readBytes().copyOf(1)
        file.writeBytes(corrupt)
        val pinFile = File(directory, "woopos-pins/$SITE_ID.pins")
        val pins = pinFile.readBytes()
        val limiter = createLimiter()
        val session = createSession(limiter)

        // WHEN
        session.initialize()
        enter(session, ADMIN_PIN)

        // THEN
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.STORAGE)
        assertThat(session.state.value.canEnterPIN).isFalse()
        assertThat(session.activeRole).isNull()
        assertThat(limiter.authenticate(SITE_ID, ADMIN_PIN)).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
        assertThat(file.readBytes()).isEqualTo(corrupt)
        assertThat(pinFile.readBytes()).isEqualTo(pins)
        assertThat(store.getPINs(SITE_ID).getOrThrow()).containsKeys(WooPosLocalRole.ADMIN, WooPosLocalRole.CASHIER)
    }

    @Test
    fun missingKeystoreKeyKeepsProtectionEnabledAndSessionLocked(): Unit = runBlocking(Dispatchers.Main) {
        // GIVEN
        val file = File(directory, "woopos-pins/$SITE_ID.pins")
        val original = file.readBytes()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_ALIAS)
        val limiter = createLimiter()
        val session = createSession(limiter)

        // WHEN
        session.initialize()
        enter(session, ADMIN_PIN)

        // THEN
        assertThat(session.isEnabled).isTrue()
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.STORAGE)
        assertThat(session.activeRole).isNull()
        assertThat(limiter.authenticate(SITE_ID, ADMIN_PIN)).isEqualTo(WooPosLocalRateLimiterResult.Unavailable)
        assertThat(file.readBytes()).isEqualTo(original)
    }

    @Test
    fun failedAttemptCommitCannotPublishAuthenticatedRole(): Unit = runBlocking(Dispatchers.Main) {
        // GIVEN
        val limiter = createLimiter()
        limiter.authenticate(SITE_ID, WRONG_PIN)
        val previous = attempts.read(SITE_ID)
        val session = createSession(limiter)
        session.initialize()
        val folder = File(directory, "woopos-pin-attempts")
        val file = File(folder, "$SITE_ID.attempts")
        check(file.setWritable(false))
        check(folder.setWritable(false))
        try {
            // WHEN
            enter(session, ADMIN_PIN)

            // THEN
            assertThat(session.state.value.isLocked).isTrue()
            assertThat(session.state.value.error).isEqualTo(WooPosLockError.STORAGE)
            assertThat(session.activeRole).isNull()
            assertThat(attempts.read(SITE_ID)).isEqualTo(previous)
        } finally {
            check(folder.setWritable(true))
            check(file.setWritable(true))
        }
        assertThat(store.getPINs(SITE_ID).getOrThrow()).containsKeys(WooPosLocalRole.ADMIN, WooPosLocalRole.CASHIER)
    }

    private fun createLimiter(pinStore: WooPosLocalPINStore = store) = WooPosLocalRateLimiter(
        pinStore,
        WooPosPINAuthenticator(pinStore, dispatchers),
        WooPosFilePINAttemptStorage(context),
        WooPosAndroidPINClock(context),
        WooPosPendingPINRecovery(),
        dispatchers,
    )

    private fun createSession(limiter: WooPosLocalRateLimiter) = WooPosAccessSession(flags, selectedSite, limiter)

    private suspend fun enter(session: WooPosAccessSession, pin: String) {
        pin.forEach { session.enterDigit(it.digitToInt()) }
    }

    @Suppress("DEPRECATION")
    private fun reportKeySecurity() {
        val key = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(KEY_ALIAS, null) as SecretKey
        val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
            .getKeySpec(key, KeyInfo::class.java) as KeyInfo
        Log.i("WooPosPINIntegration", "Hardware-backed test key: ${info.isInsideSecureHardware}")
    }

    companion object {
        private const val SITE_ID = 1
        private const val ADMIN_PIN = "1234"
        private const val CASHIER_PIN = "5678"
        private const val WRONG_PIN = "0000"
        private const val PREFERENCES = "woopos-pin-integration-test"
        private const val KEY_ALIAS = "woopos.pin-integration-test"
    }
}
