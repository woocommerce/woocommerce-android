package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.network.qrlogin.QrLoginCredentials
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.IOException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnApplicationPasswordDeleted

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginAuthenticatorTest : BaseUnitTest() {

    private val ticket = QrLoginPayload.Ticket(token = "tok", siteUrl = "https://store.example")
    private val credentials = QrLoginCredentials(
        userLogin = "admin",
        applicationPassword = "ap-secret",
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

    private val repo: WPApiSiteRepository = mock()
    private val siteStore: SiteStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val accountRepository: AccountRepository = mock()

    private val authenticator = QrLoginAuthenticator(repo, siteStore, selectedSite, accountRepository)

    @Before
    fun setUp() = testBlocking {
        whenever(repo.fetchSite(ticket.siteUrl, credentials.userLogin, credentials.applicationPassword))
            .thenReturn(Result.success(site))
        whenever(repo.checkIfUserIsEligible(site)).thenReturn(Result.success(true))
        whenever(siteStore.deleteApplicationPassword(site)).thenReturn(OnApplicationPasswordDeleted(site))
    }

    @Test
    fun `given happy path, when completeLogin, then returns site id and persists credentials`() = testBlocking {
        val result = authenticator.completeLogin(ticket, credentials)

        assertThat(result).isEqualTo(Result.success(42))
        verify(repo).saveApplicationPassword(
            localSiteId = 42,
            username = credentials.userLogin,
            password = credentials.applicationPassword
        )
        verify(selectedSite).set(site)
    }

    @Test
    fun `given site has no WooCommerce, when completeLogin, then NotAWooSite and AP not saved`() = testBlocking {
        whenever(repo.fetchSite(ticket.siteUrl, credentials.userLogin, credentials.applicationPassword))
            .thenReturn(Result.success(nonWooSite))

        val result = authenticator.completeLogin(ticket, credentials)

        assertThat(result.exceptionOrNull()).isInstanceOf(QrLoginAuthenticationException.NotAWooSite::class.java)
        verify(repo, never()).saveApplicationPassword(any(), any(), any())
        verifyNoInteractions(selectedSite)
    }

    @Test
    fun `given user is not eligible, when completeLogin, then UserNotEligible and site not selected`() = testBlocking {
        whenever(repo.checkIfUserIsEligible(site)).thenReturn(Result.success(false))

        val result = authenticator.completeLogin(ticket, credentials)

        assertThat(result.exceptionOrNull())
            .isInstanceOf(QrLoginAuthenticationException.UserNotEligible::class.java)
        verifyNoInteractions(selectedSite)
    }

    @Test
    fun `given user is not eligible, when completeLogin, then saved AP is revoked`() = testBlocking {
        whenever(repo.checkIfUserIsEligible(site)).thenReturn(Result.success(false))

        authenticator.completeLogin(ticket, credentials)

        verify(repo).saveApplicationPassword(
            localSiteId = site.id,
            username = credentials.userLogin,
            password = credentials.applicationPassword
        )
        verify(siteStore).deleteApplicationPassword(site)
        verify(accountRepository).logout()
    }

    @Test
    fun `given eligibility check fails, when completeLogin, then saved AP is revoked`() = testBlocking {
        whenever(repo.checkIfUserIsEligible(site)).thenReturn(Result.failure(IOException("offline")))

        authenticator.completeLogin(ticket, credentials)

        verify(siteStore).deleteApplicationPassword(site)
        verify(accountRepository).logout()
    }

    @Test
    fun `given fetchSite fails, when completeLogin, then failure propagates and AP not saved`() = testBlocking {
        whenever(repo.fetchSite(ticket.siteUrl, credentials.userLogin, credentials.applicationPassword))
            .thenReturn(Result.failure(IllegalStateException("nope")))

        val result = authenticator.completeLogin(ticket, credentials)

        assertThat(result.isFailure).isTrue()
        verify(repo, never()).saveApplicationPassword(any(), any(), any())
        verifyNoInteractions(selectedSite)
    }

    @Test
    fun `given fetchSite throws CancellationException, when completeLogin, then it propagates unwrapped`() =
        testBlocking {
            whenever(repo.fetchSite(ticket.siteUrl, credentials.userLogin, credentials.applicationPassword))
                .thenAnswer { throw CancellationException("cancelled") }

            var thrown: Throwable? = null
            try {
                authenticator.completeLogin(ticket, credentials)
            } catch (t: Throwable) {
                thrown = t
            }

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }

    @Test
    fun `given eligibility check fails with IO error, when completeLogin, then UserNotEligible carries the cause`() =
        testBlocking {
            val cause = IOException("offline")
            whenever(repo.checkIfUserIsEligible(site)).thenReturn(Result.failure(cause))

            val result = authenticator.completeLogin(ticket, credentials)

            val failure = result.exceptionOrNull()
            assertThat(failure).isInstanceOf(QrLoginAuthenticationException.UserNotEligible::class.java)
            assertThat((failure as QrLoginAuthenticationException.UserNotEligible).original).isEqualTo(cause)
        }
}
