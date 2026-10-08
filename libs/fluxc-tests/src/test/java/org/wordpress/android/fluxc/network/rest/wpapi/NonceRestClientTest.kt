package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Header
import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseLogger
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints.AdminBaseVerification
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.util.AppLog
import java.util.Date
import javax.net.ssl.SSLHandshakeException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import org.wordpress.android.fluxc.network.rest.Header as ResponseHeader

class NonceRestClientTest {
    private val requestBuilder: WPAPIEncodedBodyRequestBuilder = mock()
    private val currentTimeProvider: CurrentTimeProvider = mock()
    private val appLogWrapper: AppLogWrapper = mock()
    private lateinit var subject: NonceRestClient

    @Before
    fun setUp() {
        subject = NonceRestClient(
            requestBuilder,
            currentTimeProvider,
            appLogWrapper,
            mock<Dispatcher>(),
            mock<RequestQueue>(),
            mock<UserAgent>()
        )
        whenever(currentTimeProvider.currentDate()).thenReturn(Date(RESPONSE_TIME))
    }

    @Test
    fun `given core login markup, when requesting a nonce, then authenticate and cache`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce("$SITE_ORIGIN/", USERNAME, PASSWORD)

        assertEquals(Nonce.Available(EXPECTED_NONCE, USERNAME), actual)
        assertEquals(actual, subject.getNonce(SITE_ORIGIN, USERNAME))
        listOf(
            "username" to (SITE_ORIGIN to "other-user"),
            "subdirectory" to ("$SITE_HOST/other-store" to USERNAME),
            "scheme" to ("http://site.example/store" to USERNAME),
            "host" to ("https://other.example/store" to USERNAME),
            "effective port" to ("https://site.example:8443/store" to USERNAME)
        ).forEach { (label, identity) ->
            assertNull(subject.getNonce(identity.first, identity.second), label)
        }
        verifyCredentialPost(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given a custom login endpoint, when requesting a nonce, then post only to its verified form`() = test {
        givenLoginForm(CUSTOM_LOGIN_URL)
        givenCredentialRedirect(CUSTOM_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertIs<Nonce.Available>(actual)
        verifyCredentialPost(CUSTOM_LOGIN_URL, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given login fields in separate forms, when preflighting, then never post credentials`() = test {
        givenLoginForm(
            CUSTOM_LOGIN_URL,
            """
            <form name="loginform" id="loginform" method="post">
                <input type="text" name="log" id="user_login">
            </form>
            <form name="loginform" id="loginform" method="post">
                <input type="password" name="pwd" id="user_pass">
            </form>
            """.trimIndent()
        )

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual, "split forms")
        verifyNoCredentialPost()
    }

    @Test
    fun `given a dashboard contains decoy login fields, when preflighting, then never post credentials`() = test {
        val dashboardWithDecoyForm = ADMIN_DASHBOARD.replace(
            "</body>",
            "${loginForm()}</body>"
        )
        givenGet(CUSTOM_LOGIN_URL, WPAPIResponse.Success(dashboardWithDecoyForm, emptyList()))

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL, actual, "dashboard decoy")
        verifyNoCredentialPost()
    }

    @Test
    fun `given an inert template contains decoy login fields, when preflighting, then never post credentials`() = test {
        givenLoginForm(
            CUSTOM_LOGIN_URL,
            "<template>${loginForm("action=\"https://attacker.example/login\"")}</template>"
        )

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual, "template decoy")
        verifyNoCredentialPost()
    }

    @Test
    fun `given comment and script decoys, when preflighting, then only a rendered login form is used`() = test {
        val decoys = "<!-- ${loginForm()} --><script>const decoy = `${loginForm()}`;</script>"
        givenLoginForm(CUSTOM_LOGIN_URL, decoys)

        val decoyOnly = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, decoyOnly, "decoy only")
        verifyNoCredentialPost()

        val submissionUrl = "$SITE_ORIGIN/authenticate"
        givenLoginForm(CUSTOM_LOGIN_URL, decoys + loginForm("action=\"$submissionUrl\""))
        givenCredentialRedirect(submissionUrl, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenNonce(DEFAULT_NONCE_URL)

        assertIs<Nonce.Available>(subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        ))
        verifyCredentialPost(submissionUrl, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given two credential forms, when preflighting, then never post credentials`() = test {
        givenLoginForm(CUSTOM_LOGIN_URL, loginForm() + loginForm("action=\"$SITE_ORIGIN/authenticate\""))

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual, "ambiguous forms")
        verifyNoCredentialPost()
    }

    @Test
    fun `given hidden credential decoys in a core form, when preflighting, then never post credentials`() = test {
        givenLoginForm(
            CUSTOM_LOGIN_URL,
            """
            <form name="loginform" id="loginform" method="post">
                <input type="hidden" name="log" id="user_login">
                <input type="hidden" name="pwd" id="user_pass">
            </form>
            """.trimIndent()
        )

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual, "hidden credential decoys")
        verifyNoCredentialPost()
    }

    @Test
    fun `given duplicate enabled core username fields, when preflighting, then never post credentials`() = test {
        givenLoginForm(
            CUSTOM_LOGIN_URL,
            """
            <form name="loginform" id="loginform" method="post">
                <input type="text" name="log" id="user_login">
                <input type="text" name="log" id="user_login">
                <input type="password" name="pwd" id="user_pass">
            </form>
            """.trimIndent()
        )

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual, "duplicate username fields")
        verifyNoCredentialPost()
    }

    @Test
    fun `given WPS Hide Login core markup, when authenticating, then use login action`() = test {
        val submissionUrl = "$SITE_ORIGIN/hidden-login/?provider=wps"
        givenLoginForm(
            CUSTOM_LOGIN_URL,
            """
            <form role="search" method="get" action="/"><input type="search" name="s"></form>
            <form name="loginform" id="loginform" action="/store/hidden-login/?provider=wps" method="post">
                <p><input type="text" name="log" id="user_login" autocomplete="username" required></p>
                <p><input type='password' name='pwd' id='user_pass' autocomplete='current-password' required /></p>
                <input type="hidden" name="redirect_to" value="/wp-admin/">
            </form>
            """.trimIndent()
        )
        givenCredentialRedirect(submissionUrl, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertIs<Nonce.Available>(actual)
        verifyCredentialPost(submissionUrl, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given a safe relative form action, when authenticating, then post to its decoded exact target`() = test {
        val submissionUrl = "$SITE_ORIGIN/session?mode=login&source=app"
        givenLoginForm(
            CUSTOM_LOGIN_URL,
            loginForm("action=\"/store/session?mode=login&amp;source=app\"")
        )
        givenCredentialRedirect(submissionUrl, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertIs<Nonce.Available>(actual)
        verifyCredentialPost(submissionUrl, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given a safe absolute form action, when authenticating, then post to its exact target`() = test {
        val submissionUrl = "$SITE_HOST/store/authenticate"
        givenLoginForm(CUSTOM_LOGIN_URL, loginForm("action=\"$submissionUrl\""))
        givenCredentialRedirect(submissionUrl, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertIs<Nonce.Available>(actual)
        verifyCredentialPost(submissionUrl, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given a missing form action after redirects, when authenticating, then post to the final page URL`() = test {
        val finalLoginUrl = "$SITE_HOST/auth/login/"
        givenGet(CUSTOM_LOGIN_URL, redirect(finalLoginUrl))
        givenLoginForm(finalLoginUrl, loginForm())
        givenCredentialRedirect(finalLoginUrl, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertIs<Nonce.Available>(actual)
        verifyCredentialPost(finalLoginUrl, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given unsafe form actions, when preflighting, then never post credentials`() = test {
        mapOf(
            "malformed" to "https://[invalid",
            "userinfo" to "https://user:password@site.example/login",
            "off-origin" to "https://attacker.example/login",
            "downgrade" to "http://site.example/login",
            "port" to "https://site.example:8443/login"
        ).forEach { (label, action) ->
            givenLoginForm(CUSTOM_LOGIN_URL, loginForm("action=\"$action\""))

            val actual = subject.requestNonce(
                CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
                USERNAME,
                PASSWORD
            )

            assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual, label)
        }
        verifyNoCredentialPost()
    }

    @Test
    fun `given a non-POST login form, when preflighting, then never post credentials`() = test {
        givenLoginForm(CUSTOM_LOGIN_URL, loginForm(method = "get"))

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual, "GET form")
        verifyNoCredentialPost()
    }

    @Test
    fun `given WPS reaches the expected dashboard URL, when requesting a nonce, then never post credentials`() =
        test {
            val origin = "http://127.0.0.1:18080"
            val loginUrl = "$origin/login"
            val normalizedLoginUrl = "$loginUrl/"
            val dashboardUrl = "$origin/wp-admin/"
            val nonceUrl = "${dashboardUrl}admin-ajax.php?action=rest-nonce"
            givenGet(loginUrl, redirect(normalizedLoginUrl))
            givenGet(normalizedLoginUrl, redirect(dashboardUrl))
            givenGet(dashboardUrl, WPAPIResponse.Success(HOME_PAGE, emptyList()))
            givenNonce(nonceUrl)

            val actual = subject.requestNonce(
                CookieNonceAuthenticationEndpoints(origin, loginEntryUrl = loginUrl),
                USERNAME,
                PASSWORD
            )

            assertIs<Nonce.Available>(actual)
            verifyNoCredentialPost()
            verify(requestBuilder).syncGetRequest(subject, nonceUrl)
        }

    @Test
    fun `given WPS reaches admin index, when requesting a nonce, then treat it as the reusable admin base`() = test {
        val dashboardIndexUrl = "$SITE_ORIGIN/wp-admin/index.php"
        givenGet(DEFAULT_LOGIN_URL, redirect(dashboardIndexUrl))
        givenGet(dashboardIndexUrl, WPAPIResponse.Success(ADMIN_DASHBOARD, emptyList()))
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD)

        assertIs<Nonce.Available>(actual)
        verifyNoCredentialPost()
        verify(requestBuilder).syncGetRequest(subject, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given WPS reaches admin base without trailing slash, when requesting a nonce, then reuse it`() = test {
        val dashboardUrl = "$SITE_ORIGIN/wp-admin"
        givenGet(DEFAULT_LOGIN_URL, redirect(dashboardUrl))
        givenGet(dashboardUrl, WPAPIResponse.Success(ADMIN_DASHBOARD, emptyList()))
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD)

        assertIs<Nonce.Available>(actual)
        verifyNoCredentialPost()
        verify(requestBuilder).syncGetRequest(subject, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given preflight reaches an unexpected dashboard URL, when requesting a nonce, then reject it`() = test {
        givenGet(DEFAULT_LOGIN_URL, redirect(CUSTOM_ADMIN_URL))
        givenGet(CUSTOM_ADMIN_URL, WPAPIResponse.Success(ADMIN_DASHBOARD, emptyList()))
        givenNonce(DEFAULT_NONCE_URL)

        val actual = subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD)

        assertFailed(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL, actual, "unexpected dashboard")
        verifyNoCredentialPost()
        verify(requestBuilder, never()).syncGetRequest(subject, DEFAULT_NONCE_URL)
    }

    @Test
    fun `given manual admin recovery, when dashboard and derived nonce are valid, then authenticate`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialRedirect(DEFAULT_LOGIN_URL, CUSTOM_NONCE_URL, CUSTOM_NONCE_URL)
        givenGet(CUSTOM_ADMIN_URL, WPAPIResponse.Success(ADMIN_DASHBOARD, emptyList()))
        givenNonce(CUSTOM_NONCE_URL)

        val actual = subject.requestNonce(MANUAL_ADMIN_ENDPOINTS, USERNAME, PASSWORD)

        assertIs<Nonce.Available>(actual)
        verify(requestBuilder).syncGetRequest(subject, CUSTOM_ADMIN_URL)
        verify(requestBuilder).syncGetRequest(subject, CUSTOM_NONCE_URL)
    }

    @Test
    fun `given manual admin recovery, when dashboard proof is arbitrary, then reject before nonce`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialRedirect(DEFAULT_LOGIN_URL, CUSTOM_NONCE_URL, CUSTOM_NONCE_URL)
        givenGet(CUSTOM_ADMIN_URL, WPAPIResponse.Success(HOME_PAGE, emptyList()))

        val actual = subject.requestNonce(MANUAL_ADMIN_ENDPOINTS, USERNAME, PASSWORD)

        assertFailed(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL, actual, "manual dashboard")
        verify(requestBuilder, never()).syncGetRequest(subject, CUSTOM_NONCE_URL)
    }

    @Test
    fun `given arbitrary successful login content, when preflighting, then never post credentials`() = test {
        givenGet(CUSTOM_LOGIN_URL, WPAPIResponse.Success(HOME_PAGE, emptyList()))

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, actual, "soft 2xx")
        assertEquals(false, (actual as Nonce.FailedRequest).loginEntryVerified)
        verifyNoCredentialPost()
    }

    @Test
    fun `given verified custom login, when later requests fail, then retain login provenance`() = test {
        givenLoginForm(CUSTOM_LOGIN_URL)
        val endpoints = CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL)

        givenCredentialResponse(
            CUSTOM_LOGIN_URL,
            DEFAULT_NONCE_URL,
            WPAPIResponse.Success(HOME_PAGE, emptyList())
        )
        val responseFailure = subject.requestNonce(endpoints, USERNAME, PASSWORD)
        assertFailed(Nonce.CookieNonceErrorType.INVALID_RESPONSE, responseFailure, "post response")
        assertEquals(true, (responseFailure as Nonce.FailedRequest).loginEntryVerified)

        val networkError = mock<WPAPINetworkError>()
        networkError.volleyError = NoConnectionError()
        givenCredentialResponse(CUSTOM_LOGIN_URL, DEFAULT_NONCE_URL, WPAPIResponse.Error(networkError))
        val networkFailure = subject.requestNonce(endpoints, USERNAME, PASSWORD)
        assertEquals(Nonce.Unknown(USERNAME, loginEntryVerified = true), networkFailure)

        givenCredentialRedirect(CUSTOM_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenGet(DEFAULT_NONCE_URL, WPAPIResponse.Success("x", emptyList()))
        val nonceFailure = subject.requestNonce(endpoints, USERNAME, PASSWORD)
        assertFailed(Nonce.CookieNonceErrorType.INVALID_NONCE, nonceFailure, "nonce response")
        assertEquals(true, (nonceFailure as Nonce.FailedRequest).loginEntryVerified)
    }

    @Test
    fun `given unsafe endpoint inputs, when requesting a nonce, then reject without network access`() = test {
        mapOf(
            "userinfo" to "https://user:password@site.example/login",
            "host" to "https://attacker.example/login",
            "downgrade" to "http://site.example/login",
            "port" to "https://site.example:8443/login"
        ).forEach { (label, unsafeLogin) ->
            val actual = subject.requestNonce(
                CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = unsafeLogin),
                USERNAME,
                PASSWORD
            )

            assertFailed(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL, actual, label)
            verify(requestBuilder, never()).syncGetRequest(subject, unsafeLogin)
        }
    }

    @Test
    fun `given unsafe preflight redirects, when requesting a nonce, then reject without posting credentials`() = test {
        mapOf(
            "host" to "https://attacker.example/login",
            "downgrade" to "http://site.example/login",
            "port" to "https://site.example:8443/login",
            "userinfo" to "https://user@site.example/login"
        ).forEach { (label, unsafeRedirect) ->
            givenGet(CUSTOM_LOGIN_URL, redirect(unsafeRedirect))

            val actual = subject.requestNonce(
                CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
                USERNAME,
                PASSWORD
            )

            assertFailed(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL, actual, label)
        }
        verifyNoCredentialPost()
    }

    @Test
    fun `given more than three safe redirects, when preflighting, then stop before the fourth destination`() = test {
        val redirects = (1..4).map { "$SITE_HOST/login/$it" }
        givenGet(CUSTOM_LOGIN_URL, redirect(redirects[0]))
        redirects.zipWithNext().forEach { (from, to) -> givenGet(from, redirect(to)) }

        val actual = subject.requestNonce(
            CookieNonceAuthenticationEndpoints(SITE_ORIGIN, loginEntryUrl = CUSTOM_LOGIN_URL),
            USERNAME,
            PASSWORD
        )

        assertFailed(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL, actual, "redirect cap")
        assertEquals(
            UnexpectedStoreResponseKind.UNEXPECTED_CONTENT,
            (actual as Nonce.FailedRequest).unexpectedStoreResponse?.kind
        )
        verify(requestBuilder, never()).syncGetRequest(subject, redirects.last())
        verifyNoCredentialPost()
    }

    @Test
    fun `given exact and mismatched nonce redirects, when posting credentials, then follow only the derived URL`() =
        test {
            givenLoginForm(DEFAULT_LOGIN_URL)
            givenCredentialRedirect(
                DEFAULT_LOGIN_URL,
                DEFAULT_NONCE_URL,
                "/store/wp-admin/admin-ajax.php?action=rest-nonce"
            )
            givenNonce(DEFAULT_NONCE_URL)
            assertIs<Nonce.Available>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

            mapOf(
                "literal custom admin" to (
                    "$SITE_HOST/private-admin/admin-ajax.php?action=rest-nonce" to
                        Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL
                    ),
                "path lookalike" to (
                    "$SITE_HOST/private-admin/not-admin-ajax.php?action=rest-nonce" to
                        Nonce.CookieNonceErrorType.INVALID_NONCE
                    ),
                "wrong action" to (
                    "$SITE_HOST/private-admin/admin-ajax.php?action=other" to
                        Nonce.CookieNonceErrorType.INVALID_NONCE
                    ),
                "extra query" to (
                    "$SITE_HOST/private-admin/admin-ajax.php?action=rest-nonce&extra=1" to
                        Nonce.CookieNonceErrorType.INVALID_NONCE
                    )
            ).forEach { (label, case) ->
                givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, case.first)

                val actual = subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD)

                assertFailed(case.second, actual, label)
                verify(requestBuilder, never()).syncGetRequest(subject, case.first)
            }
        }

    @Test
    fun `given HTTP safely upgrades to HTTPS, when authenticating, then keep the transaction on HTTPS`() = test {
        val httpOrigin = "http://site.example"
        val httpLoginUrl = "$httpOrigin/wp-login.php"
        val httpsLoginUrl = "$SITE_HOST/wp-login.php"
        val httpsNonceUrl = "$SITE_HOST/wp-admin/admin-ajax.php?action=rest-nonce"
        givenGet(httpLoginUrl, redirect(httpsLoginUrl))
        givenLoginForm(httpsLoginUrl)
        givenCredentialRedirect(httpsLoginUrl, httpsNonceUrl, httpsNonceUrl)
        givenNonce(httpsNonceUrl)

        val actual = subject.requestNonce(httpOrigin, USERNAME, PASSWORD)

        assertIs<Nonce.Available>(actual)
        verifyCredentialPost(httpsLoginUrl, httpsNonceUrl)
    }

    @Test
    fun `given invalid credentials, when posting, then preserve the extracted authentication error`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialResponse(
            DEFAULT_LOGIN_URL,
            DEFAULT_NONCE_URL,
            WPAPIResponse.Success(
                """
                <script>${NonceRestClient.INVALID_CREDENTIAL_HTML_PATTERN}</script>
                <div id="login_error"><strong>Error:</strong> Incorrect password.
                <a href="lost">Lost?</a></div>
                """.trimIndent(),
                emptyList()
            )
        )

        val actual = subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD)

        assertFailed(Nonce.CookieNonceErrorType.INVALID_CREDENTIALS, actual, "invalid credentials")
        assertEquals("Error: Incorrect password.", (actual as Nonce.FailedRequest).errorMessage)
    }

    @Test
    fun `given Basic Auth or no connection, when preflighting, then preserve the failure type`() = test {
        givenGet(DEFAULT_LOGIN_URL, error(401, listOf(Header("WWW-Authenticate", "Basic realm=restricted"))))
        assertFailed(
            Nonce.CookieNonceErrorType.BASIC_AUTH_REQUIRED,
            subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD),
            "basic auth"
        )

        val networkError = mock<WPAPINetworkError>()
        networkError.volleyError = NoConnectionError()
        givenGet(DEFAULT_LOGIN_URL, WPAPIResponse.Error(networkError))
        assertEquals(
            Nonce.Unknown(USERNAME),
            subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD),
            "network error"
        )

        givenGet(DEFAULT_LOGIN_URL, error(500))
        val serverFailure = subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD)
        assertFailed(Nonce.CookieNonceErrorType.GENERIC_ERROR, serverFailure, "server error")
        assertEquals(false, (serverFailure as Nonce.FailedRequest).loginEntryVerified)
    }

    @Test
    fun `given TLS failure wrapped as no connection, when preflighting, then preserve certificate error`() = test {
        val volleyError = NoConnectionError(SSLHandshakeException("certificate rejected"))
        val networkError = WPAPINetworkError(
            BaseNetworkError(GenericErrorType.INVALID_SSL_CERTIFICATE, volleyError)
        )
        givenGet(DEFAULT_LOGIN_URL, WPAPIResponse.Error(networkError))

        val actual = subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD)

        val failure = assertIs<Nonce.FailedRequest>(actual)
        assertEquals(Nonce.CookieNonceErrorType.GENERIC_ERROR, failure.type)
        assertEquals(GenericErrorType.INVALID_SSL_CERTIFICATE, failure.networkError?.type)
    }

    @Test
    fun `given a firewall page instead of the login page, when preflighting, then keep the response details`() = test {
        givenGet(DEFAULT_LOGIN_URL, error(403, FIREWALL_PAGE, HTML_HEADERS))

        val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

        assertEquals(Nonce.CookieNonceErrorType.GENERIC_ERROR, failure.type)
        assertEquals(UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE, failure.unexpectedStoreResponse?.kind)
        assertEquals("GET /store/wp-login.php", failure.unexpectedStoreResponse?.requestType)
        assertEquals("Access Denied | Blocked by the firewall.", failure.unexpectedStoreResponse?.excerpt)
        verify(appLogWrapper).w(
            AppLog.T.API,
            UnexpectedStoreResponseLogger.buildMessage(requireNotNull(failure.unexpectedStoreResponse))
        )
    }

    @Test
    fun `given a login page without a login form, when preflighting, then keep the response details`() = test {
        givenGet(DEFAULT_LOGIN_URL, WPAPIResponse.Success(CHALLENGE_PAGE, HTML_RESPONSE_HEADERS, statusCode = 202))

        val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

        assertEquals(Nonce.CookieNonceErrorType.INVALID_RESPONSE, failure.type)
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, failure.unexpectedStoreResponse?.kind)
        assertEquals(202, failure.unexpectedStoreResponse?.statusCode)
        assertEquals("GET /store/wp-login.php", failure.unexpectedStoreResponse?.requestType)
    }

    @Test
    fun `given an unexpected page after posting credentials, when requesting a nonce, then keep the details`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialResponse(
            DEFAULT_LOGIN_URL,
            DEFAULT_NONCE_URL,
            WPAPIResponse.Success(FIREWALL_PAGE, HTML_RESPONSE_HEADERS)
        )

        val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

        assertEquals(Nonce.CookieNonceErrorType.INVALID_RESPONSE, failure.type)
        assertEquals("POST /store/wp-login.php", failure.unexpectedStoreResponse?.requestType)
    }

    @Test
    fun `given a captcha error after posting credentials, when requesting a nonce, then add no details`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialResponse(
            DEFAULT_LOGIN_URL,
            DEFAULT_NONCE_URL,
            WPAPIResponse.Success(
                "<div id=\"login_error\"><strong>Error:</strong> Please complete the captcha.</div>",
                HTML_RESPONSE_HEADERS
            )
        )

        val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

        assertEquals(Nonce.CookieNonceErrorType.INVALID_RESPONSE, failure.type)
        assertNull(failure.unexpectedStoreResponse)
    }

    @Test
    fun `given a page instead of a nonce, when requesting a nonce, then keep the response details`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenGet(DEFAULT_NONCE_URL, WPAPIResponse.Success(FIREWALL_PAGE, HTML_RESPONSE_HEADERS))

        val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

        assertEquals(Nonce.CookieNonceErrorType.INVALID_NONCE, failure.type)
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, failure.unexpectedStoreResponse?.kind)
        assertEquals("GET /store/wp-admin/admin-ajax.php", failure.unexpectedStoreResponse?.requestType)
    }

    @Test
    fun `given known login failures, when requesting a nonce, then add no response details`() = test {
        givenGet(DEFAULT_LOGIN_URL, error(404, FIREWALL_PAGE, HTML_HEADERS))
        val customLoginUrl = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertEquals(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL, customLoginUrl.type)
        assertNull(customLoginUrl.unexpectedStoreResponse, "custom login URL")

        givenGet(
            DEFAULT_LOGIN_URL,
            error(401, FIREWALL_PAGE, HTML_HEADERS + Header("WWW-Authenticate", "Basic realm=restricted"))
        )
        val basicAuth = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertEquals(Nonce.CookieNonceErrorType.BASIC_AUTH_REQUIRED, basicAuth.type)
        assertNull(basicAuth.unexpectedStoreResponse, "basic auth")

        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenGet(
            DEFAULT_NONCE_URL,
            error(401, FIREWALL_PAGE, HTML_HEADERS + Header("WWW-Authenticate", "Basic realm=restricted"))
        )
        val nonceBasicAuth = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertNull(nonceBasicAuth.unexpectedStoreResponse, "basic auth on the nonce request")

        givenGet(DEFAULT_NONCE_URL, error(410, FIREWALL_PAGE, HTML_HEADERS))
        val nonceGone = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertNull(nonceGone.unexpectedStoreResponse, "missing nonce endpoint")
    }

    @Test
    fun `given an empty error response, when preflighting, then keep the status details`() = test {
        givenGet(DEFAULT_LOGIN_URL, error(403))

        val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

        assertEquals(Nonce.CookieNonceErrorType.GENERIC_ERROR, failure.type)
        assertEquals(UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE, failure.unexpectedStoreResponse?.kind)
        assertEquals(403, failure.unexpectedStoreResponse?.statusCode)
        assertNull(failure.unexpectedStoreResponse?.excerpt)
    }

    @Test
    fun `given the login cookie isn't kept, when requesting the nonce, then keep the status details`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenGet(DEFAULT_NONCE_URL, error(400, "0"))

        val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

        assertEquals(Nonce.CookieNonceErrorType.GENERIC_ERROR, failure.type)
        assertEquals(UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE, failure.unexpectedStoreResponse?.kind)
        assertEquals(400, failure.unexpectedStoreResponse?.statusCode)
        assertEquals("GET /store/wp-admin/admin-ajax.php", failure.unexpectedStoreResponse?.requestType)
    }

    @Test
    fun `given a 404 after posting credentials, when requesting a nonce, then keep the status details`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialResponse(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, error(404, FIREWALL_PAGE, HTML_HEADERS))

        val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

        assertEquals(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL, failure.type)
        assertEquals(UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE, failure.unexpectedStoreResponse?.kind)
        assertEquals(404, failure.unexpectedStoreResponse?.statusCode)
    }

    @Test
    fun `given an error status on the page a step expects, when requesting a nonce, then add no details`() = test {
        givenGet(DEFAULT_LOGIN_URL, error(500, LOGIN_FORM))
        val loginPage = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertNull(loginPage.unexpectedStoreResponse, "login page")

        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialResponse(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, error(403, LOGIN_ERROR))
        val loginError = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertNull(loginError.unexpectedStoreResponse, "login error")

        givenCredentialRedirect(DEFAULT_LOGIN_URL, CUSTOM_NONCE_URL, CUSTOM_NONCE_URL)
        givenGet(CUSTOM_ADMIN_URL, error(500, ADMIN_DASHBOARD))
        val dashboard = assertIs<Nonce.FailedRequest>(
            subject.requestNonce(MANUAL_ADMIN_ENDPOINTS, USERNAME, PASSWORD)
        )
        assertNull(dashboard.unexpectedStoreResponse, "dashboard")
    }

    @Test
    fun `given a nonce or a login error with an error status, when requesting the nonce, then keep the status details`() = test {
        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)

        listOf(EXPECTED_NONCE, LOGIN_ERROR).forEach { body ->
            givenGet(DEFAULT_NONCE_URL, error(500, body))

            val failure = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))

            assertEquals(UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE, failure.unexpectedStoreResponse?.kind)
            assertEquals(500, failure.unexpectedStoreResponse?.statusCode)
        }
    }

    @Test
    fun `given a 2xx without what the step expects, when requesting a nonce, then keep the content details`() = test {
        givenGet(DEFAULT_LOGIN_URL, WPAPIResponse.Success("", emptyList()))
        val loginPage = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, loginPage.unexpectedStoreResponse?.kind, "login")

        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialResponse(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, WPAPIResponse.Success("{}", emptyList()))
        val credentials = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, credentials.unexpectedStoreResponse?.kind, "post")

        givenCredentialRedirect(DEFAULT_LOGIN_URL, CUSTOM_NONCE_URL, CUSTOM_NONCE_URL)
        givenGet(CUSTOM_ADMIN_URL, WPAPIResponse.Success(HOME_PAGE, emptyList()))
        val dashboard = assertIs<Nonce.FailedRequest>(
            subject.requestNonce(MANUAL_ADMIN_ENDPOINTS, USERNAME, PASSWORD)
        )
        assertEquals(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL, dashboard.type)
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, dashboard.unexpectedStoreResponse?.kind, "admin")

        givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenGet(DEFAULT_NONCE_URL, WPAPIResponse.Success("0", emptyList()))
        val nonce = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, nonce.unexpectedStoreResponse?.kind, "nonce")
    }

    @Test
    fun `given redirects the login can't follow, when requesting a nonce, then keep the redirect details`() = test {
        givenGet(DEFAULT_LOGIN_URL, redirect(SSO_LOGIN_URL))
        val loginPage = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertEquals(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL, loginPage.type)
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, loginPage.unexpectedStoreResponse?.kind, "login")
        assertEquals(302, loginPage.unexpectedStoreResponse?.statusCode)

        givenLoginForm(DEFAULT_LOGIN_URL)
        givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, "$SITE_ORIGIN/shop/")
        val credentials = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertEquals(Nonce.CookieNonceErrorType.INVALID_NONCE, credentials.type)
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, credentials.unexpectedStoreResponse?.kind, "post")
        assertEquals("POST /store/wp-login.php", credentials.unexpectedStoreResponse?.requestType)

        givenCredentialRedirect(DEFAULT_LOGIN_URL, CUSTOM_NONCE_URL, CUSTOM_NONCE_URL)
        givenGet(CUSTOM_ADMIN_URL, redirect(SSO_LOGIN_URL))
        val dashboard = assertIs<Nonce.FailedRequest>(
            subject.requestNonce(MANUAL_ADMIN_ENDPOINTS, USERNAME, PASSWORD)
        )
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, dashboard.unexpectedStoreResponse?.kind, "admin")

        givenCredentialRedirect(DEFAULT_LOGIN_URL, DEFAULT_NONCE_URL, DEFAULT_NONCE_URL)
        givenGet(DEFAULT_NONCE_URL, redirect(DEFAULT_LOGIN_URL))
        val nonce = assertIs<Nonce.FailedRequest>(subject.requestNonce(SITE_ORIGIN, USERNAME, PASSWORD))
        assertEquals(UnexpectedStoreResponseKind.UNEXPECTED_CONTENT, nonce.unexpectedStoreResponse?.kind, "nonce")
    }

    private suspend fun givenLoginForm(url: String, html: String = LOGIN_FORM) {
        givenGet(url, WPAPIResponse.Success(html, emptyList()))
    }

    private suspend fun givenCredentialRedirect(loginUrl: String, nonceUrl: String, location: String) {
        givenCredentialResponse(loginUrl, nonceUrl, redirect(location))
    }

    private suspend fun givenCredentialResponse(
        loginUrl: String,
        nonceUrl: String,
        response: WPAPIResponse<String>
    ) {
        whenever(requestBuilder.syncPostRequest(subject, loginUrl, body = credentialBody(nonceUrl)))
            .thenReturn(response)
    }

    private suspend fun givenNonce(url: String) {
        givenGet(url, WPAPIResponse.Success(EXPECTED_NONCE, emptyList()))
    }

    private suspend fun givenGet(url: String, response: WPAPIResponse<String>) {
        whenever(requestBuilder.syncGetRequest(subject, url)).thenReturn(response)
    }

    private suspend fun verifyCredentialPost(loginUrl: String, nonceUrl: String) {
        verify(requestBuilder).syncPostRequest(subject, loginUrl, body = credentialBody(nonceUrl))
    }

    private suspend fun verifyNoCredentialPost() {
        verify(requestBuilder, never()).syncPostRequest(
            restClient = any(),
            url = any(),
            params = any(),
            body = any(),
            enableCaching = any(),
            cacheTimeToLive = any(),
            nonce = anyOrNull()
        )
    }

    private fun credentialBody(nonceUrl: String) = mapOf(
        "log" to USERNAME,
        "pwd" to PASSWORD,
        "redirect_to" to nonceUrl
    )

    private fun loginForm(actionAttribute: String? = null, method: String = "post"): String {
        val action = actionAttribute?.let { " $it" }.orEmpty()
        return "<form name=\"loginform\" id=\"loginform\" method=\"$method\"$action>" +
            "<input type=\"text\" name=\"log\" id=\"user_login\">" +
            "<input type=\"password\" name=\"pwd\" id=\"user_pass\"></form>"
    }

    private fun redirect(location: String) = error(302, listOf(Header("Location", location)))

    private fun error(statusCode: Int, headers: List<Header> = emptyList()): WPAPIResponse.Error<String> =
        error(statusCode, body = "", headers = headers)

    private fun error(
        statusCode: Int,
        body: String,
        headers: List<Header> = emptyList()
    ): WPAPIResponse.Error<String> =
        WPAPIResponse.Error(
            WPAPINetworkError(
                BaseNetworkError(
                    VolleyError(
                        NetworkResponse(
                            statusCode,
                            body.toByteArray(),
                            false,
                            System.currentTimeMillis(),
                            headers
                        )
                    )
                )
            )
        )

    private fun assertFailed(expectedType: Nonce.CookieNonceErrorType, actual: Nonce, label: String) {
        assertIs<Nonce.FailedRequest>(actual, label)
        assertEquals(RESPONSE_TIME, actual.timeOfResponse, label)
        assertEquals(expectedType, actual.type, label)
    }

    private companion object {
        val HTML_HEADERS = listOf(Header("Content-Type", "text/html; charset=UTF-8"))
        val HTML_RESPONSE_HEADERS = listOf(ResponseHeader("Content-Type", "text/html; charset=UTF-8"))
        const val FIREWALL_PAGE = "<html><head><title>Access Denied</title></head>" +
            "<body><p>Blocked by the firewall.</p></body></html>"
        const val CHALLENGE_PAGE = "<html><head><title>Just a moment...</title></head>" +
            "<body><p>Checking your browser.</p></body></html>"
        const val RESPONSE_TIME = 123456L
        const val SITE_HOST = "https://site.example"
        const val SITE_ORIGIN = "$SITE_HOST/store"
        const val DEFAULT_LOGIN_URL = "$SITE_ORIGIN/wp-login.php"
        const val DEFAULT_NONCE_URL = "$SITE_ORIGIN/wp-admin/admin-ajax.php?action=rest-nonce"
        const val CUSTOM_LOGIN_URL = "$SITE_HOST/secret-login"
        const val SSO_LOGIN_URL = "https://sso.example/login"
        const val CUSTOM_ADMIN_URL = "$SITE_HOST/private-dashboard/"
        const val CUSTOM_NONCE_URL = "$SITE_HOST/private-dashboard/admin-ajax.php?action=rest-nonce"
        const val USERNAME = "a_username"
        const val PASSWORD = "a_password"
        const val EXPECTED_NONCE = "1expectedNONCE"
        const val LOGIN_FORM =
            "<form name=\"loginform\" id=\"loginform\" method=\"post\">" +
                "<input type=\"text\" name=\"log\" id=\"user_login\">" +
                "<input type=\"password\" name=\"pwd\" id=\"user_pass\"></form>"
        const val HOME_PAGE = "<html><body class=\"home page\"><main>Storefront</main></body></html>"
        const val LOGIN_ERROR = "<div id=\"login_error\"><strong>Error:</strong> Too many failed login attempts.</div>"
        const val ADMIN_DASHBOARD =
            "<html><body class=\"wp-admin wp-core-ui index-php\">" +
                "<div id=\"dashboard-widgets-wrap\"></div></body></html>"
        val MANUAL_ADMIN_ENDPOINTS = CookieNonceAuthenticationEndpoints(
            siteUrl = SITE_ORIGIN,
            adminBaseUrl = CUSTOM_ADMIN_URL,
            adminBaseVerification = AdminBaseVerification.AUTHENTICATED_DASHBOARD
        )
    }
}
