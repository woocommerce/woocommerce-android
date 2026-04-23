package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginAuthenticatorTest : BaseUnitTest() {

    private val ticket = QrLoginPayload.Ticket(token = "tok", siteUrl = "https://store.example")
    private val credentials = QrLoginCredentials(
        userLogin = "admin",
        siteUrl = "https://store.example",
        applicationPassword = "ap-secret",
        uuid = "uuid-1"
    )

    private val site = SiteModel().apply {
        id = 42
        url = "https://store.example"
        hasWooCommerce = true
    }

    private val nonWooSite = SiteModel().apply {
        id = 7
        url = "https://blog.example"
        hasWooCommerce = false
    }

    @Test
    fun `given exchange succeeds and user is eligible, when authenticate, then returns site id`() = testBlocking {
        val exchangeClient: QrLoginExchangeClient = mock()
        whenever(exchangeClient.exchange(ticket.siteUrl, ticket.token)).thenReturn(Result.success(credentials))
        val repo: WPApiSiteRepository = mock()
        whenever(repo.fetchSite(ticket.siteUrl, credentials.userLogin, credentials.applicationPassword))
            .thenReturn(Result.success(site))
        whenever(repo.checkIfUserIsEligible(site)).thenReturn(Result.success(true))
        val selectedSite: SelectedSite = mock()
        val authenticator = QrLoginAuthenticator(exchangeClient, repo, selectedSite)

        val result = authenticator.authenticate(ticket)

        assertThat(result).isEqualTo(Result.success(42))
        verify(repo).saveApplicationPassword(
            localSiteId = 42,
            username = credentials.userLogin,
            password = credentials.applicationPassword
        )
        verify(selectedSite).set(site)
    }

    @Test
    fun `given exchange fails, when authenticate, then propagates exchange exception`() = testBlocking {
        val exchangeClient: QrLoginExchangeClient = mock()
        whenever(exchangeClient.exchange(any(), any()))
            .thenReturn(Result.failure(QrLoginExchangeException.RateLimited))
        val repo: WPApiSiteRepository = mock()
        val selectedSite: SelectedSite = mock()
        val authenticator = QrLoginAuthenticator(exchangeClient, repo, selectedSite)

        val result = authenticator.authenticate(ticket)

        assertThat(result.exceptionOrNull()).isEqualTo(QrLoginExchangeException.RateLimited)
        verifyNoInteractions(repo)
        verifyNoInteractions(selectedSite)
    }

    @Test
    fun `given site has no WooCommerce, when authenticate, then NotAWooSite without saving AP`() = testBlocking {
        val exchangeClient: QrLoginExchangeClient = mock()
        whenever(exchangeClient.exchange(any(), any())).thenReturn(Result.success(credentials))
        val repo: WPApiSiteRepository = mock()
        whenever(repo.fetchSite(any(), any(), any())).thenReturn(Result.success(nonWooSite))
        val selectedSite: SelectedSite = mock()
        val authenticator = QrLoginAuthenticator(exchangeClient, repo, selectedSite)

        val result = authenticator.authenticate(ticket)

        assertThat(result.exceptionOrNull()).isInstanceOf(QrLoginAuthenticationException.NotAWooSite::class.java)
        verify(repo, never()).saveApplicationPassword(any(), any(), any())
        verifyNoInteractions(selectedSite)
    }

    @Test
    fun `given user is not eligible, when authenticate, then UserNotEligible without selecting site`() = testBlocking {
        val exchangeClient: QrLoginExchangeClient = mock()
        whenever(exchangeClient.exchange(any(), any())).thenReturn(Result.success(credentials))
        val repo: WPApiSiteRepository = mock()
        whenever(repo.fetchSite(any(), any(), any())).thenReturn(Result.success(site))
        whenever(repo.checkIfUserIsEligible(site)).thenReturn(Result.success(false))
        val selectedSite: SelectedSite = mock()
        val authenticator = QrLoginAuthenticator(exchangeClient, repo, selectedSite)

        val result = authenticator.authenticate(ticket)

        assertThat(result.exceptionOrNull())
            .isInstanceOf(QrLoginAuthenticationException.UserNotEligible::class.java)
        verifyNoInteractions(selectedSite)
    }

    @Test
    fun `given fetchSite fails, when authenticate, then failure propagates and AP not saved`() = testBlocking {
        val exchangeClient: QrLoginExchangeClient = mock()
        whenever(exchangeClient.exchange(any(), any())).thenReturn(Result.success(credentials))
        val repo: WPApiSiteRepository = mock()
        whenever(repo.fetchSite(any(), any(), any())).thenReturn(Result.failure(IllegalStateException("nope")))
        val selectedSite: SelectedSite = mock()
        val authenticator = QrLoginAuthenticator(exchangeClient, repo, selectedSite)

        val result = authenticator.authenticate(ticket)

        assertThat(result.isFailure).isTrue()
        verify(repo, never()).saveApplicationPassword(any(), any(), any())
        verifyNoInteractions(selectedSite)
    }
}
