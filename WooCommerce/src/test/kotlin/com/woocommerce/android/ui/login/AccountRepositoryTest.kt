package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.FakeDispatcher
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.sitepicker.sitevisibility.VisibleWooSitesDataStore
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
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
    private val posDataStore: DataStore<Preferences> = mock()
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
        posDataStore = posDataStore
    )

    @Test
    fun `given signed in using wordpress_com, when logout is called, then unregister device from push notifications`() =
        testBlocking {
            // GIVEN
            given(accountStore.hasAccessToken()).willReturn(true)

            // WHEN
            repository.logout()

            // THEN
            verify(pushNotificationRepository).unregisterDeviceFromPushNotifications()
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
