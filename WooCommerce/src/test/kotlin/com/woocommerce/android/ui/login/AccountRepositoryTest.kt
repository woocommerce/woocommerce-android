package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.FakeDispatcher
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.sitepicker.sitevisibility.VisibleWooSitesDataStore
import com.woocommerce.android.ui.woopos.lock.WooPosLockedPOSStore
import com.woocommerce.android.ui.woopos.lock.WooPosPINAccess
import com.woocommerce.android.ui.woopos.roles.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.WooPosPINError
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.yield
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AccountError
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnApplicationPasswordDeleted
import org.wordpress.android.fluxc.store.account.CloseAccountStore
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryTest : BaseUnitTest() {
    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock()
    private val closeAccountStore: CloseAccountStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val zendeskSettings: ZendeskSettings = mock()
    private val appPrefs: AppPrefs = mock()
    private val visibleWooSitesDataStore: VisibleWooSitesDataStore = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()
    private val registerDevice: RegisterDevice = mock()
    private val posDataStore: DataStore<Preferences> = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val localPINStore: WooPosLocalPINStore = mock {
        on { clearAll() } doReturn Result.success(Unit)
    }
    private val pinAccess: WooPosPINAccess = mock {
        on { clearAllAttempts() } doReturn Result.success(Unit)
    }
    private val lockedPOS: WooPosLockedPOSStore = mock {
        on { clearAll() } doReturn Result.success(Unit)
    }
    private val appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher)
    private val dispatcher = FakeDispatcher().apply {
        registerActionHandler(AccountAction.SIGN_OUT) {
            appCoroutineScope.launch {
                yield()
                emitChange(AccountStore.OnAccountChanged())
            }
        }
    }

    private val repository = AccountRepository(
        accountStore = accountStore,
        siteStore = siteStore,
        closeAccountStore = closeAccountStore,
        selectedSite = selectedSite,
        dispatcher = dispatcher,
        zendeskSettings = zendeskSettings,
        prefs = appPrefs,
        appCoroutineScope = appCoroutineScope,
        siteVisibilityDataStore = visibleWooSitesDataStore,
        dispatchers = coroutinesTestRule.testDispatchers,
        pushNotificationRepository = pushNotificationRepository,
        registerDevice = registerDevice,
        posDataStore = posDataStore,
        analyticsTracker = analyticsTracker,
        localPINStore = localPINStore,
        pinAccess = pinAccess,
        lockedPOS = lockedPOS,
    )

    @Test
    fun `when logging out without a session, then stale local PINs are cleared`() = testBlocking {
        // WHEN
        assertThat(repository.logout()).isTrue()

        // THEN
        verify(localPINStore).clearAll()
        verify(pinAccess).clearAllAttempts()
        verify(lockedPOS).clearAll()
    }

    @Test
    fun `given PIN erasure fails after sign out, when logging out, then logout still succeeds`() = testBlocking {
        // GIVEN
        given(accountStore.hasAccessToken()).willReturn(true)
        whenever(localPINStore.clearAll()).thenReturn(Result.failure(WooPosPINError.StorageFailure))

        // WHEN
        assertThat(repository.logout()).isTrue()

        // THEN
        verify(localPINStore).clearAll()
        verify(pinAccess).clearAllAttempts()
        verify(lockedPOS).clearAll()
        verify(selectedSite).reset()
    }

    @Test
    fun `given sign out fails, when logging out, then local PIN protection is preserved`() = testBlocking {
        // GIVEN
        given(accountStore.hasAccessToken()).willReturn(true)
        dispatcher.registerActionHandler(AccountAction.SIGN_OUT) {
            appCoroutineScope.launch {
                yield()
                emitChange(AccountStore.OnAccountChanged().apply {
                    error = AccountError(AccountErrorType.GENERIC_ERROR, "Sign out failed")
                    causeOfChange = AccountAction.SIGN_OUT
                })
            }
        }

        // WHEN
        assertThat(repository.logout()).isFalse()

        // THEN
        verify(localPINStore, never()).clearAll()
        verify(lockedPOS, never()).clearAll()
        verify(selectedSite, never()).reset()
    }

    @Test
    fun `given marker erasure fails after sign out, when logging out, then logout still succeeds`() = testBlocking {
        // GIVEN
        given(accountStore.hasAccessToken()).willReturn(true)
        whenever(lockedPOS.clearAll()).thenReturn(Result.failure(IllegalStateException()))

        // WHEN
        assertThat(repository.logout()).isTrue()

        // THEN
        verify(lockedPOS).clearAll()
        verify(selectedSite).reset()
    }

    @Test
    fun `given account closes but PIN erasure fails, when closing account, then closure remains successful`() = testBlocking {
        // GIVEN
        whenever(closeAccountStore.closeAccount()).thenReturn(CloseAccountStore.CloseAccountResult())
        whenever(localPINStore.clearAll()).thenReturn(Result.failure(WooPosPINError.StorageFailure))

        // WHEN
        assertThat(repository.closeAccount()).isEqualTo(AccountRepository.CloseAccountResult.Success)

        // THEN
        verify(localPINStore).clearAll()
        verify(pinAccess).clearAllAttempts()
        verify(lockedPOS).clearAll()
        verify(selectedSite).reset()
    }

    @Test
    fun `given successful sign out, when caller is cancelled during password cleanup, then PINs are still cleared`() = testBlocking {
        // GIVEN
        given(accountStore.hasAccessToken()).willReturn(true)
        lateinit var logout: Job
        given(siteStore.sitesAccessedViaWPComRest).willAnswer {
            logout.cancel()
            emptyList<SiteModel>()
        }

        // WHEN
        logout = launch(start = CoroutineStart.LAZY) { repository.logout() }
        logout.start()
        advanceUntilIdle()

        // THEN
        assertThat(logout.isCancelled).isTrue()
        verify(localPINStore).clearAll()
        verify(pinAccess).clearAllAttempts()
        verify(lockedPOS).clearAll()
        verify(selectedSite).reset()
    }

    @Test
    fun `given sign out was dispatched, when caller is cancelled before its result, then PIN cleanup completes`() = testBlocking {
        // GIVEN
        given(accountStore.hasAccessToken()).willReturn(true)
        val signOutGate = CompletableDeferred<Unit>()
        dispatcher.registerActionHandler(AccountAction.SIGN_OUT) {
            appCoroutineScope.launch {
                signOutGate.await()
                emitChange(AccountStore.OnAccountChanged())
            }
        }

        // WHEN
        val logout = launch { repository.logout() }
        runCurrent()
        logout.cancel()
        signOutGate.complete(Unit)
        advanceUntilIdle()

        // THEN
        verify(localPINStore).clearAll()
        verify(pinAccess).clearAllAttempts()
        verify(lockedPOS).clearAll()
        verify(selectedSite).reset()
    }

    @Test
    fun `given PIN erasure is suspended, when logging out, then site removal waits for erasure`() = testBlocking {
        // GIVEN
        given(accountStore.hasAccessToken()).willReturn(true)
        val gate = CompletableDeferred<Unit>()
        whenever(localPINStore.clearAll()).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }

        // WHEN
        val result = async { repository.logout() }
        runCurrent()

        // THEN
        assertThat(result.isCompleted).isFalse()
        verify(selectedSite, never()).reset()
        gate.complete(Unit)
        assertThat(result.await()).isTrue()
        inOrder(localPINStore, pinAccess, lockedPOS, selectedSite) {
            verify(localPINStore).clearAll()
            verify(pinAccess).clearAllAttempts()
            verify(lockedPOS).clearAll()
            verify(selectedSite).reset()
        }
    }

    @Test
    fun `given user is logged in, when logged out involuntarily, then track the event with the reason`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(true)

            // WHEN
            repository.logoutInvoluntarily(InvoluntaryLogoutReason.INVALID_TOKEN)

            // THEN
            verify(analyticsTracker).track(
                AnalyticsEvent.ACCOUNT_INVOLUNTARY_LOGOUT,
                mapOf(AnalyticsTracker.KEY_REASON to "invalid_token")
            )
            verify(pushNotificationRepository).unregisterDeviceFromPushNotifications()
        }

    @Test
    fun `given user is not logged in, when logged out involuntarily, then don't track the event`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(false)
            given(selectedSite.connectionType).willReturn(null)

            // WHEN
            repository.logoutInvoluntarily(InvoluntaryLogoutReason.INVALID_TOKEN)

            // THEN
            verify(analyticsTracker, never()).track(any(), any<Map<String, *>>())
        }

    @Test
    fun `given signed in using wordpress_com, when logout is called, then cancel in-progress push registration before unregistering the device`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(true)

            // WHEN
            repository.logout()

            // THEN
            inOrder(registerDevice, pushNotificationRepository) {
                verify(registerDevice).cancelInProgressRegistration()
                verify(pushNotificationRepository).unregisterDeviceFromPushNotifications()
            }
        }

    @Test
    fun `given signed in using wordpress_com, when logout is called, then remove app passwords of user sites`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(true)
            val sites = List(3) { SiteModel().apply { siteId = it.toLong() } }
            given(siteStore.sitesAccessedViaWPComRest).willReturn(sites)
            val sitesDeleted = mutableListOf<SiteModel>()
            given(siteStore.deleteApplicationPassword(any())).willAnswer {
                val site = it.getArgument(0) as SiteModel
                sitesDeleted.add(site)
                OnApplicationPasswordDeleted(site)
            }

            // WHEN
            repository.logout()
            advanceUntilIdle()

            // THEN
            assertThat(sitesDeleted).containsExactlyElementsOf(sites)
        }

    @Test
    fun `given signed in using app password, when logout is called, then unregister push notifications and delete selected site password`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(false)
            given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
            val selectedSiteModel = SiteModel()
            given(selectedSite.getOrNull()).willReturn(selectedSiteModel)
            given(siteStore.deleteApplicationPassword(selectedSiteModel))
                .willReturn(OnApplicationPasswordDeleted(selectedSiteModel))

            // WHEN
            repository.logout()

            // THEN
            verify(pushNotificationRepository).unregisterDeviceFromPushNotifications()
            verify(siteStore).deleteApplicationPassword(selectedSiteModel)
        }

    @Test
    fun `given signed in using app password, when push cleanup is suspended, then logout waits for it before succeeding`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(false)
            given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
            val selectedSiteModel = SiteModel()
            given(selectedSite.getOrNull()).willReturn(selectedSiteModel)
            given(siteStore.deleteApplicationPassword(selectedSiteModel))
                .willReturn(OnApplicationPasswordDeleted(selectedSiteModel))
            val cleanupGate = CompletableDeferred<Unit>()
            whenever(pushNotificationRepository.unregisterDeviceFromPushNotifications()).doSuspendableAnswer {
                cleanupGate.await()
            }

            // WHEN
            val result = async { repository.logout() }
            runCurrent()

            // THEN
            assertThat(result.isCompleted).isFalse()

            cleanupGate.complete(Unit)
            result.await()
            advanceUntilIdle()
            assertThat(result.isCompleted).isTrue()
        }

    @Test
    fun `given app password login, when the site is reset during push cleanup, then the captured site password is deleted`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(false)
            given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
            val selectedSiteModel = SiteModel()
            given(selectedSite.getOrNull()).willReturn(selectedSiteModel)
            given(siteStore.deleteApplicationPassword(selectedSiteModel))
                .willReturn(OnApplicationPasswordDeleted(selectedSiteModel))
            val cleanupGate = CompletableDeferred<Unit>()
            whenever(pushNotificationRepository.unregisterDeviceFromPushNotifications()).doSuspendableAnswer {
                cleanupGate.await()
            }

            // WHEN
            val result = async { repository.logout() }
            runCurrent()
            assertThat(result.isCompleted).isFalse()
            // A concurrent flow resets the selected site while the push cleanup is still in flight.
            // Stubbed leniently: the fix must not touch the selected site again after resuming.
            lenient().doReturn(null).whenever(selectedSite).getOrNull()
            lenient().doThrow(SelectedSite.SelectedSiteResetException()).whenever(selectedSite).get()
            cleanupGate.complete(Unit)

            // THEN
            assertThat(result.await()).isTrue()
            advanceUntilIdle()
            verify(siteStore).deleteApplicationPassword(selectedSiteModel)
            verify(selectedSite, never()).get()
            verify(selectedSite).reset()
        }

    @Test
    fun `given app password login, when the password deletion is in flight, then the selected site is reset only after it completes`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(false)
            given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
            val selectedSiteModel = SiteModel()
            given(selectedSite.getOrNull()).willReturn(selectedSiteModel)
            val deletionGate = CompletableDeferred<Unit>()
            whenever(siteStore.deleteApplicationPassword(selectedSiteModel)).doSuspendableAnswer {
                deletionGate.await()
                OnApplicationPasswordDeleted(selectedSiteModel)
            }

            // WHEN
            val result = async { repository.logout() }
            runCurrent()

            // THEN
            assertThat(result.isCompleted).isFalse()
            verify(selectedSite, never()).reset()

            deletionGate.complete(Unit)
            assertThat(result.await()).isTrue()
            verify(selectedSite).reset()
        }

    @Test
    fun `given app password login, when the password deletion hangs, then logout completes after the timeout`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(false)
            given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
            val selectedSiteModel = SiteModel()
            given(selectedSite.getOrNull()).willReturn(selectedSiteModel)
            val deletionGate = CompletableDeferred<Unit>()
            whenever(siteStore.deleteApplicationPassword(selectedSiteModel)).doSuspendableAnswer {
                deletionGate.await()
                OnApplicationPasswordDeleted(selectedSiteModel)
            }

            // WHEN
            val result = async { repository.logout() }
            advanceTimeBy(5_001L)

            // THEN
            assertThat(result.isCompleted).isTrue()
            assertThat(result.await()).isTrue()
            verify(selectedSite).reset()
            deletionGate.complete(Unit)
        }

    @Test
    fun `given app password login and no selected site, when logout is called, then no password is deleted`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(false)
            given(selectedSite.connectionType).willReturn(SiteConnectionType.ApplicationPasswords)
            given(selectedSite.getOrNull()).willReturn(null)

            // WHEN
            val result = repository.logout()
            advanceUntilIdle()

            // THEN
            assertThat(result).isTrue()
            verify(selectedSite).reset()
            verify(siteStore, never()).deleteApplicationPassword(any())
        }
}
