package com.woocommerce.android.ui.login

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.woocommerce.android.FakeDispatcher
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator.CookieNonceAuthenticationResult.Error
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator.CookieNonceAuthenticationResult.Success
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.GENERIC_ERROR
import java.net.CookieManager

@OptIn(ExperimentalCoroutinesApi::class)
class WPApiSiteRepositoryTest : BaseUnitTest() {
    private val site = SiteModel().apply {
        id = SITE_ID
        loginUrl = ORIGINAL_LOGIN_URL
        adminUrl = ORIGINAL_ADMIN_URL
    }
    private val siteStore: SiteStore = mock()
    private val authenticator: CookieNonceAuthenticator = mock {
        on { authenticate(ENDPOINTS, USERNAME, PASSWORD) } doReturn Success
    }
    private val testScope = CoroutineScope(coroutinesTestRule.testDispatcher)
    private var updateEvent = OnSiteChanged(rowsAffected = 1)
    private val dispatcher = FakeDispatcher().apply {
        registerActionHandler(SiteAction.UPDATE_SITE) {
            testScope.launch {
                yield()
                emitChange(updateEvent)
            }
        }
    }
    private val repository = WPApiSiteRepository(
        dispatcher,
        siteStore,
        authenticator,
        mock<UserEligibilityFetcher>(),
        mock<ApplicationPasswordsNotifier>(),
        CookieManager(),
        mock<ApplicationPasswordsStore>()
    )

    @Test
    fun `given structured endpoints, when logging in, then forward them to native authentication`() = testBlocking {
        val result = repository.login(SITE_URL, USERNAME, PASSWORD, ENDPOINTS)

        assertThat(result.isSuccess).isTrue()
        verify(authenticator).authenticate(ENDPOINTS, USERNAME, PASSWORD)
    }

    @Test
    fun `given verified login entry returns HTTP error, when logging in, then do not blame custom URL`() =
        testBlocking {
            whenever(authenticator.authenticate(ENDPOINTS, USERNAME, PASSWORD)).thenReturn(
                Error(
                    type = CUSTOM_LOGIN_URL,
                    networkError = BaseNetworkError(
                        VolleyError(NetworkResponse(404, byteArrayOf(), false, 0, emptyList()))
                    ),
                    loginEntryVerified = true
                )
            )

            val exception = repository.login(SITE_URL, USERNAME, PASSWORD, ENDPOINTS).exceptionOrNull()
                as WPApiSiteRepository.CookieNonceAuthenticationException

            assertThat(exception.errorMessage).isEqualTo(
                UiStringRes(R.string.login_site_credentials_http_error, listOf(UiStringText("404")))
            )
            assertThat(exception.loginEntryVerified).isTrue()
        }

    @Test
    fun `given direct WPAPI certificate failure, when logging in, then show security certificate guidance`() =
        testBlocking {
            whenever(authenticator.authenticate(ENDPOINTS, USERNAME, PASSWORD)).thenReturn(
                Error(
                    type = UNKNOWN,
                    message = "low-level TLS exception",
                    networkError = BaseNetworkError(INVALID_SSL_CERTIFICATE)
                )
            )

            val exception = repository.login(SITE_URL, USERNAME, PASSWORD, ENDPOINTS).exceptionOrNull()
                as WPApiSiteRepository.CookieNonceAuthenticationException

            assertThat(exception.errorMessage).isEqualTo(UiStringRes(R.string.error_site_url_remote_certificate))
            assertThat(exception.message).isNull()
        }

    @Test
    fun `given direct WPAPI offline failure, when logging in, then hide low-level details behind generic error`() =
        testBlocking {
            whenever(authenticator.authenticate(ENDPOINTS, USERNAME, PASSWORD)).thenReturn(
                Error(
                    type = UNKNOWN,
                    message = "socket details",
                    networkError = BaseNetworkError(NO_CONNECTION)
                )
            )

            val exception = repository.login(SITE_URL, USERNAME, PASSWORD, ENDPOINTS).exceptionOrNull()
                as WPApiSiteRepository.CookieNonceAuthenticationException

            assertThat(exception.errorMessage).isEqualTo(UiStringRes(R.string.error_generic))
            assertThat(exception.message).isNull()
        }

    @Test
    fun `given dispatch error, when saving proven endpoints, then fail without mutating caller`() = testBlocking {
        updateEvent = OnSiteChanged(SiteError(GENERIC_ERROR))

        val result = repository.saveAuthenticationEndpoints(site, ENDPOINTS)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(OnChangedException::class.java)
        thenOriginalEndpointsAreRetained()
    }

    @Test
    fun `given update affects zero rows, when saving proven endpoints, then fail without mutating caller`() =
        testBlocking {
            updateEvent = OnSiteChanged(rowsAffected = 0)

            val result = repository.saveAuthenticationEndpoints(site, ENDPOINTS)

            assertThat(result.isFailure).isTrue()
            thenOriginalEndpointsAreRetained()
        }

    @Test
    fun `given update persists but site cannot be reloaded, when saving endpoints, then fail without rollback`() =
        testBlocking {
            val result = repository.saveAuthenticationEndpoints(site, ENDPOINTS)

            assertThat(result.isFailure).isTrue()
            assertThat(site.loginUrl).isEqualTo(LOGIN_URL)
            assertThat(site.adminUrl).isEqualTo(ADMIN_URL)
        }

    @Test
    fun `given persistence succeeds, when saving proven endpoints, then return distinct reloaded site`() = testBlocking {
        val reloaded = SiteModel().apply {
            id = SITE_ID
            loginUrl = LOGIN_URL
            adminUrl = ADMIN_URL
        }
        whenever(siteStore.getSiteByLocalId(SITE_ID)).thenReturn(reloaded)

        val result = repository.saveAuthenticationEndpoints(site, ENDPOINTS)

        assertThat(site.loginUrl).isEqualTo(LOGIN_URL)
        assertThat(site.adminUrl).isEqualTo(ADMIN_URL)
        assertThat(result.getOrNull()).isSameAs(reloaded)
        assertThat(result.getOrNull()).isNotSameAs(site)
    }

    private fun thenOriginalEndpointsAreRetained() {
        assertThat(site.loginUrl).isEqualTo(ORIGINAL_LOGIN_URL)
        assertThat(site.adminUrl).isEqualTo(ORIGINAL_ADMIN_URL)
    }

    private companion object {
        const val SITE_ID = 7
        const val SITE_URL = "https://example.com"
        const val LOGIN_URL = "$SITE_URL/private-login"
        const val ADMIN_URL = "$SITE_URL/private-admin/"
        const val ORIGINAL_LOGIN_URL = "$SITE_URL/wp-login.php"
        const val ORIGINAL_ADMIN_URL = "$SITE_URL/wp-admin/"
        const val USERNAME = "merchant"
        const val PASSWORD = "password"
        val ENDPOINTS = CookieNonceAuthenticationEndpoints(SITE_URL, LOGIN_URL, ADMIN_URL)
    }
}
