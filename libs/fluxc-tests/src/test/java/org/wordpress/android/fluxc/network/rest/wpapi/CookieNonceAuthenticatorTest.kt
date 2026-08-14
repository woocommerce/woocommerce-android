package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CookieNonceAuthenticatorTest {
    private val nonceClient: NonceRestClient = mock()
    private val discoveryClient: DiscoveryWPAPIRestClient = mock()
    private val siteStore: SiteStore = mock()
    private lateinit var subject: CookieNonceAuthenticator

    @Before
    fun setUp() {
        subject = CookieNonceAuthenticator(nonceClient, discoveryClient, siteStore, initCoroutineEngine())
    }

    @Test
    fun `given structured endpoints, when authenticating, then require an available nonce`() = test {
        whenever(nonceClient.requestNonce(ENDPOINTS, USERNAME, PASSWORD))
            .thenReturn(Nonce.Available(NEW_NONCE, USERNAME))

        val actual = subject.authenticate(ENDPOINTS, USERNAME, PASSWORD)

        assertIs<CookieNonceAuthenticator.CookieNonceAuthenticationResult.Success>(actual)
        verify(nonceClient).requestNonce(ENDPOINTS, USERNAME, PASSWORD)
    }

    @Test
    fun `given verified login entry failure, when authenticating, then propagate provenance`() = test {
        whenever(nonceClient.requestNonce(ENDPOINTS, USERNAME, PASSWORD))
            .thenReturn(nonceFailure(loginEntryVerified = true))

        val actual = subject.authenticate(ENDPOINTS, USERNAME, PASSWORD)

        val error = assertIs<CookieNonceAuthenticator.CookieNonceAuthenticationResult.Error>(actual)
        assertTrue(error.loginEntryVerified)
    }

    @Test
    fun `given initial nonce acquisition fails, when making a protected request, then return before REST`() = test {
        val site = site()
        whenever(nonceClient.getNonce(SITE_URL, USERNAME)).thenReturn(null)
        whenever(nonceClient.requestNonce(CookieNonceAuthenticationEndpoints.from(site), USERNAME, PASSWORD))
            .thenReturn(nonceFailure())
        var restCalls = 0

        val actual = subject.makeAuthenticatedWPAPIRequest(site) {
            restCalls++
            WPAPIResponse.Success("unexpected", emptyList())
        }

        assertEquals(0, restCalls)
        assertNonceFailureHasNoResponse(actual)
        verify(siteStore, never()).insertOrUpdateSite(site)
    }

    @Test
    fun `given nonce acquisition times out, when making a protected request, then preserve transport error`() = test {
        val site = site()
        whenever(nonceClient.getNonce(SITE_URL, USERNAME)).thenReturn(null)
        whenever(nonceClient.requestNonce(CookieNonceAuthenticationEndpoints.from(site), USERNAME, PASSWORD))
            .thenReturn(
                nonceFailure(
                    networkError = WPAPINetworkError(
                        BaseNetworkError(GenericErrorType.TIMEOUT, TIMEOUT_MESSAGE)
                    )
                )
            )

        val actual = subject.makeAuthenticatedWPAPIRequest(site) {
            WPAPIResponse.Success("unexpected", emptyList())
        }

        val error = assertIs<WPAPIResponse.Error<*>>(actual).error
        assertEquals(GenericErrorType.TIMEOUT, error.type)
        assertEquals(TIMEOUT_MESSAGE, error.message)
        assertNull(error.volleyError)
    }

    @Test
    fun `given cached nonce gets 401 and refresh fails, when making a protected request, then call REST once`() = test {
        val site = site()
        whenever(nonceClient.getNonce(SITE_URL, USERNAME)).thenReturn(Nonce.Available(OLD_NONCE, USERNAME))
        whenever(nonceClient.requestNonce(CookieNonceAuthenticationEndpoints.from(site), USERNAME, PASSWORD))
            .thenReturn(nonceFailure())
        var restCalls = 0

        val actual = subject.makeAuthenticatedWPAPIRequest(site) {
            restCalls++
            unauthorized<String>()
        }

        assertEquals(1, restCalls)
        assertNonceFailureHasNoResponse(actual)
    }

    @Test
    fun `given saved custom endpoints, when acquiring a nonce, then use both endpoint values`() = test {
        val site = site().apply {
            loginUrl = LOGIN_URL
            adminUrl = ADMIN_URL
        }
        whenever(nonceClient.getNonce(SITE_URL, USERNAME)).thenReturn(null)
        whenever(nonceClient.requestNonce(ENDPOINTS, USERNAME, PASSWORD))
            .thenReturn(Nonce.Available(NEW_NONCE, USERNAME))

        val actual = subject.makeAuthenticatedWPAPIRequest(site) {
            WPAPIResponse.Success("response", emptyList())
        }

        assertIs<WPAPIResponse.Success<String>>(actual)
        verify(nonceClient).requestNonce(ENDPOINTS, USERNAME, PASSWORD)
    }

    private fun site() = SiteModel().apply {
        url = SITE_URL
        username = USERNAME
        password = PASSWORD
        wpApiRestUrl = WP_API_URL
    }

    private fun nonceFailure(
        loginEntryVerified: Boolean = false,
        networkError: WPAPINetworkError = WPAPINetworkError(
            BaseNetworkError(VolleyError(NetworkResponse(404, byteArrayOf(), true, 0, emptyList())))
        )
    ) = Nonce.FailedRequest(
        timeOfResponse = 1L,
        username = USERNAME,
        type = Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL,
        networkError = networkError,
        loginEntryVerified = loginEntryVerified
    )

    private fun <T> unauthorized(): WPAPIResponse<T> = WPAPIResponse.Error(
        WPAPINetworkError(
            BaseNetworkError(VolleyError(NetworkResponse(401, byteArrayOf(), true, 0, emptyList())))
        )
    )

    private fun assertNonceFailureHasNoResponse(actual: WPAPIResponse<*>) {
        val error = assertIs<WPAPIResponse.Error<*>>(actual).error
        assertNull(error.volleyError?.networkResponse)
    }

    private companion object {
        const val SITE_URL = "https://site.example/store"
        const val WP_API_URL = "https://site.example/wp-json/"
        const val LOGIN_URL = "https://site.example/private-login"
        const val ADMIN_URL = "https://site.example/private-admin/"
        const val USERNAME = "merchant"
        const val PASSWORD = "password"
        const val OLD_NONCE = "oldNonce"
        const val NEW_NONCE = "newNonce"
        const val TIMEOUT_MESSAGE = "Request timed out"
        val ENDPOINTS = CookieNonceAuthenticationEndpoints(SITE_URL, LOGIN_URL, ADMIN_URL)
    }
}
