package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NonceRestClientTest {
    private val wpApiEncodedRequestBuilder: WPAPIEncodedBodyRequestBuilder = mock()
    private val currentTimeProvider: CurrentTimeProvider = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var subject: NonceRestClient
    private val time = 123456L

    private val site = SiteModel().apply {
        url = "asiteurl.com"
        username = "a_username"
        password = "a_password"
    }
    private val nonceRequestUrl = "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"

    @Before
    fun setUp() {
        subject = NonceRestClient(wpApiEncodedRequestBuilder, currentTimeProvider, dispatcher, requestQueue, userAgent)
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(time))
    }

    @Test
    fun `successful nonce request`() = test {
        val redirectResponse = WPAPIResponse.Error<String>(
            WPAPINetworkError(
                BaseNetworkError(
                    VolleyError(
                        NetworkResponse(
                            301,
                            byteArrayOf(),
                            false,
                            System.currentTimeMillis(),
                            listOf(com.android.volley.Header("Location", nonceRequestUrl))
                        )
                    )
                ),
                null
            )
        )
        val expectedNonce = "1expectedNONCE"
        givenLoginResponse(redirectResponse)
        givenNonceRequestResponse(WPAPIResponse.Success(expectedNonce, emptyList()))

        val actual = subject.requestNonce(site)

        TestCase.assertEquals(Nonce.Available(expectedNonce, site.username), actual)
    }

    @Test
    fun `invalid credentials returns correct error message`() = test {
        @Suppress("MaxLineLength")
        val loginResponse = WPAPIResponse.Success(
            """
            <html>
              <script>${NonceRestClient.INVALID_CREDENTIAL_HTML_PATTERN}</script>
              <head>
                    <div id="login_error">
                        <strong>Error:</strong> The password you entered for the username <strong>demo</strong> is incorrect. <a href="link/">Lost your password?</a><br>
                    </div>
              </head>
            </html>
        """.trimIndent(), emptyList()
        )
        givenLoginResponse(loginResponse)

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.INVALID_CREDENTIALS, actual.type)
        assertEquals("Error: The password you entered for the username demo is incorrect.", actual.errorMessage)
    }

    @Test
    fun `when error message mentions captcha, treat error as invalid response`() = test {
        @Suppress("MaxLineLength")
        val loginResponse = WPAPIResponse.Success(
            """
            <html>
              <script>${NonceRestClient.INVALID_CREDENTIAL_HTML_PATTERN}</script>
              <head>
                    <div id="login_error">Please enter the captcha to continue</div>
              </head>
            </html>
        """.trimIndent(), emptyList()
        )
        givenLoginResponse(loginResponse)

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual.type)
        assertEquals("Please enter the captcha to continue", actual.errorMessage)
    }

    @Test
    fun `invalid nonce of '0' returns FailedRequest`() = test {
        val redirectUrl = "${site.url}/wp-admin/admin-ajax.php?action=rest-nonce"

        val redirectResponse = WPAPIResponse.Error<String>(
            WPAPINetworkError(
                BaseNetworkError(
                    VolleyError(
                        NetworkResponse(
                            301,
                            byteArrayOf(),
                            false,
                            System.currentTimeMillis(),
                            listOf(com.android.volley.Header("Location", redirectUrl))
                        )
                    )
                ),
                null
            )
        )

        val invalidNonce = "0"
        val response = WPAPIResponse.Success(invalidNonce, emptyList())
        givenLoginResponse(redirectResponse)
        whenever(wpApiEncodedRequestBuilder.syncGetRequest(subject, redirectUrl))
            .thenReturn(response)

        val actual = subject.requestNonce(site)
        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(time, actual.timeOfResponse)
        assertEquals(Nonce.CookieNonceErrorType.INVALID_NONCE, actual.type)
    }

    @Test
    fun `failed nonce request return FailedRequest`() = test {
        val baseNetworkError = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(400, byteArrayOf(), false, System.currentTimeMillis(), listOf())
                )
            )
        )
        givenLoginResponse(WPAPIResponse.Error(baseNetworkError))

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(time, actual.timeOfResponse)
        assertEquals(Nonce.CookieNonceErrorType.GENERIC_ERROR, actual.type)
        assertEquals(baseNetworkError, actual.networkError)
    }

    @Test
    fun `failed nonce request with connection error returns Unknown`() = test {
        val baseNetworkError = mock<WPAPINetworkError>()
        baseNetworkError.volleyError = NoConnectionError()
        givenLoginResponse(WPAPIResponse.Error(baseNetworkError))

        val actual = subject.requestNonce(site)
        TestCase.assertEquals(Nonce.Unknown(site.username), actual)
    }

    @Test
    fun `custom login URL returns correct error type`() = test {
        val error = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(
                        404,
                        byteArrayOf(),
                        false,
                        System.currentTimeMillis(),
                        listOf()
                    )
                )
            )
        )
        givenLoginResponse(WPAPIResponse.Error(error))

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL, actual.type)
    }

    @Test
    fun `custom admin URL returns correct error type`() = test {
        val redirectResponse = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(
                        301,
                        byteArrayOf(),
                        false,
                        System.currentTimeMillis(),
                        listOf(com.android.volley.Header("Location", nonceRequestUrl))
                    )
                )
            ),
            null
        )
        val nonceError = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(404, byteArrayOf(), false, System.currentTimeMillis(), listOf())
                )
            )
        )
        givenLoginResponse(WPAPIResponse.Error(redirectResponse))
        givenNonceRequestResponse(WPAPIResponse.Error(nonceError))

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL, actual.type)
    }

    @Test
    fun `given http site that 302s to https on wp-login, when requesting nonce, then retries POST against https`() =
        test {
            val httpSite = "http://example.com"
            val httpsSite = "https://example.com"
            val nonceUrl = "$httpsSite/wp-admin/admin-ajax.php?action=rest-nonce"
            givenPostRedirect(siteUrl = httpSite, location = "$httpsSite/wp-login.php")
            givenPostRedirect(siteUrl = httpsSite, location = nonceUrl)
            whenever(wpApiEncodedRequestBuilder.syncGetRequest(subject, nonceUrl))
                .thenReturn(WPAPIResponse.Success("expectedNONCE", emptyList()))

            val actual = subject.requestNonce(httpSite, "user", "pwd")

            assertIs<Nonce.Available>(actual)
            assertEquals("expectedNONCE", actual.value)
        }

    @Test
    fun `given redirect to a different host, when requesting nonce, then returns INVALID_NONCE without following`() =
        test {
            val httpSite = "http://example.com"
            givenPostRedirect(siteUrl = httpSite, location = "https://attacker.example/wp-login.php")

            val actual = subject.requestNonce(httpSite, "user", "pwd")

            assertIs<Nonce.FailedRequest>(actual)
            assertEquals(Nonce.CookieNonceErrorType.INVALID_NONCE, actual.type)
        }

    @Test
    fun `given a second scheme upgrade redirect, when requesting nonce, then recursion guard returns INVALID_NONCE`() =
        test {
            val httpSite = "http://example.com"
            val httpsSite = "https://example.com"
            givenPostRedirect(siteUrl = httpSite, location = "$httpsSite/wp-login.php")
            // After the upgrade retry, the upgraded host redirects back to itself again — pathological
            // case that should NOT recurse a third time.
            givenPostRedirect(siteUrl = httpsSite, location = "$httpsSite/wp-login.php")

            val actual = subject.requestNonce(httpSite, "user", "pwd")

            assertIs<Nonce.FailedRequest>(actual)
        }

    @Test
    fun `when basic auth required, then NetworkResponse returns correct error type`() = test {
        val error = WPAPINetworkError(
            BaseNetworkError(
                VolleyError(
                    NetworkResponse(
                        401,
                        byteArrayOf(),
                        false,
                        System.currentTimeMillis(),
                        listOf(
                            com.android.volley.Header(
                                "www-Authenticate",
                                "Basic realm=token24433434"
                            )
                        )
                    )
                )
            )
        )
        givenLoginResponse(WPAPIResponse.Error(error))

        val actual = subject.requestNonce(site)

        assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.BASIC_AUTH_REQUIRED, actual.type)
    }

    private suspend fun givenLoginResponse(response: WPAPIResponse<String>) {
        val body = mapOf(
            "log" to site.username,
            "pwd" to site.password,
            "redirect_to" to nonceRequestUrl
        )

        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, "${site.url}/wp-login.php", body = body))
            .thenReturn(response)
    }

    private suspend fun givenNonceRequestResponse(response: WPAPIResponse<String>) {
        whenever(wpApiEncodedRequestBuilder.syncGetRequest(subject, nonceRequestUrl))
            .thenReturn(response)
    }

    private suspend fun givenPostRedirect(siteUrl: String, location: String, statusCode: Int = 302) {
        val wpLoginUrl = "$siteUrl/wp-login.php"
        val nonceRedirectUrl = "$siteUrl/wp-admin/admin-ajax.php?action=rest-nonce"
        val body = mapOf("log" to "user", "pwd" to "pwd", "redirect_to" to nonceRedirectUrl)
        val response = WPAPIResponse.Error<String>(
            WPAPINetworkError(
                BaseNetworkError(
                    VolleyError(
                        NetworkResponse(
                            statusCode,
                            byteArrayOf(),
                            false,
                            System.currentTimeMillis(),
                            listOf(com.android.volley.Header("Location", location))
                        )
                    )
                ),
                null
            )
        )
        whenever(wpApiEncodedRequestBuilder.syncPostRequest(subject, wpLoginUrl, body = body))
            .thenReturn(response)
    }
}
