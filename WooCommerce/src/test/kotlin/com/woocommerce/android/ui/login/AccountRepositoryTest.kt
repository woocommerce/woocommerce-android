package com.woocommerce.android.ui.login

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.FakeDispatcher
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.sitepicker.sitevisibility.VisibleWooSitesDataStore
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.SiteStore
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
    private val dispatcher = FakeDispatcher().apply {
        registerActionHandler(AccountAction.SIGN_OUT) {
            emitChange(AccountStore.OnAccountChanged())
        }
    }
    private val appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher)

    private val repository = AccountRepository(
        accountStore = accountStore,
        siteStore = siteStore,
        closeAccountStore = closeAccountStore,
        selectedSite = selectedSite,
        dispatcher = dispatcher,
        zendeskSettings = zendeskSettings,
        prefs = appPrefs,
        appCoroutineScope = appCoroutineScope,
        siteVisibilityDataStore = visibleWooSitesDataStore
    )

    @Test
    fun `given signed in using wordpress_com, when logout is called, then unregister device`() = testBlocking {
        given(accountStore.hasAccessToken()).willReturn(true)
        var deviceUnregistered = false
        dispatcher.registerActionHandler(NotificationAction.UNREGISTER_DEVICE) {
            deviceUnregistered = true
            emitChange(NotificationStore.OnDeviceUnregistered())
        }

        repository.logout()

        assertThat(deviceUnregistered).isTrue()
    }
}
