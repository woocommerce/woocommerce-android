package com.woocommerce.android.ui.login.auto

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.store.AccountStore

@Suppress("FunctionNaming")
@OptIn(ExperimentalCoroutinesApi::class)
class AutoLoginHandlerTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val accountStore: AccountStore = mock()
    private val credentialsStore: ApplicationPasswordsStore = mock()
    private val directStrategy: ApplicationPasswordAutoLoginStrategy = mock()
    private val wpComStrategy: WPComAutoLoginStrategy = mock()
    private val account = AccountModel()
    private val handler = AutoLoginHandler(
        selectedSite,
        accountStore,
        credentialsStore,
        AutoLoginSiteMatcher(),
        directStrategy,
        wpComStrategy
    )

    @Before
    fun setUp() {
        whenever(selectedSite.getIfExists()).thenReturn(null)
        whenever(selectedSite.getSelectedSiteId()).thenReturn(-1)
        whenever(accountStore.account).thenReturn(account)
        whenever(accountStore.hasAccessToken()).thenReturn(false)
    }

    @Test
    fun `when matching direct session is selected, then it is already active`() = testBlocking {
        val site = directSite()
        whenever(selectedSite.getIfExists()).thenReturn(site)
        whenever(credentialsStore.hasCredentials(site)).thenReturn(true)

        assertThat(handler.login(request(AutoLoginConnection.WP_API))).isEqualTo(AutoLoginResult.AlreadyActive)
        verify(directStrategy, never()).login(any())
    }

    @Test
    fun `when selected session is incomplete or mismatched, then it conflicts`() = testBlocking {
        whenever(selectedSite.getIfExists()).thenReturn(directSite())
        assertThat(handler.login(request(AutoLoginConnection.WP_API)))
            .isEqualTo(AutoLoginResult.Failure(AutoLoginStatus.CONFLICT))

        whenever(selectedSite.getIfExists()).thenReturn(directSite().apply {
            url = "https://store.example/shopper"
        })
        assertThat(handler.login(request(AutoLoginConnection.WP_API)))
            .isEqualTo(AutoLoginResult.Failure(AutoLoginStatus.CONFLICT))

        whenever(selectedSite.getIfExists()).thenReturn(directSite().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
        })
        assertThat(handler.login(request(AutoLoginConnection.WPCOM)))
            .isEqualTo(AutoLoginResult.Failure(AutoLoginStatus.CONFLICT))

        whenever(selectedSite.getIfExists()).thenReturn(directSite())
        account.userId = ACCOUNT_ID
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        assertThat(handler.login(request(AutoLoginConnection.WPCOM)))
            .isEqualTo(AutoLoginResult.Failure(AutoLoginStatus.CONFLICT))

        verify(directStrategy, never()).login(any())
        verify(wpComStrategy, never()).login(any(), any())
    }

    @Test
    fun `when matching WPCom session is selected, then it is already active`() = testBlocking {
        whenever(selectedSite.getIfExists()).thenReturn(directSite().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
        })
        account.userId = ACCOUNT_ID
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        assertThat(handler.login(request(AutoLoginConnection.WPCOM))).isEqualTo(AutoLoginResult.AlreadyActive)
        verify(wpComStrategy, never()).login(any(), any())
    }

    @Test
    fun `when session is clean, then direct login is routed`() = testBlocking {
        val request = request(AutoLoginConnection.WP_API)
        whenever(directStrategy.login(request)).thenReturn(ApplicationPasswordAutoLoginStrategy.Result.Success)

        assertThat(handler.login(request)).isEqualTo(AutoLoginResult.Success)
        verify(directStrategy).login(request)
    }

    @Test
    fun `when coherent WPCom session has no selected site, then it is reused`() = testBlocking {
        account.userId = ACCOUNT_ID
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        val request = request(AutoLoginConnection.WPCOM)
        whenever(wpComStrategy.login(request, reuseExistingSession = true))
            .thenReturn(WPComAutoLoginStrategy.Result.Success)

        assertThat(handler.login(request)).isEqualTo(AutoLoginResult.Success)
        verify(wpComStrategy).login(request, reuseExistingSession = true)
    }

    private fun request(connection: AutoLoginConnection) =
        ApplicationPasswordAutoLoginStrategyTest.request(connection)

    private fun directSite() = ApplicationPasswordAutoLoginStrategyTest.site().apply {
        username = ApplicationPasswordAutoLoginStrategyTest.USERNAME
    }

    private companion object {
        const val ACCOUNT_ID = 42L
    }
}
