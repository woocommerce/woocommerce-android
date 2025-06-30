package com.woocommerce.android.ui.common.webview

import android.webkit.CookieManager
import android.webkit.WebView
import com.woocommerce.android.extensions.loginUrlOrDefault
import com.woocommerce.android.extensions.urlEncode
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.compose.component.web.WCWebViewEvent
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore

@OptIn(ExperimentalCoroutinesApi::class)
class WebViewAuthenticatorTest : BaseUnitTest() {
    private val authenticationFlowResolver = mock<WebViewAuthenticationFlowResolver>()
    private val selectedSite = mock<SelectedSite>()
    private val accountStore = mock<AccountStore>()
    private val webViewCookieManager = mock<CookieManager>()

    private val sut = WebViewAuthenticator(
        authenticationFlowResolver = authenticationFlowResolver,
        selectedSite = selectedSite,
        accountStore = accountStore,
        webViewCookieManager = webViewCookieManager
    )

    @Test
    fun `given WPCom authentication, when authenticating and loading a URL, then it should post the Bearer token`() =
        testBlocking {
            val webView = mock<WebView>()
            val url = "https://wordpress.com/test"
            givenAuthenticationFlow(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.WPCom)
            givenWPComCredentials()

            sut.authenticateAndLoadUrl(webView, url, emptyFlow())

            verify(webView).postUrl(
                eq(WebViewAuthenticator.WPCOM_LOGIN_URL),
                argThat {
                    val content = toString(Charsets.UTF_8)
                    content.contains("log=$DEFAULT_USERNAME") &&
                        content.contains("authorization=${"Bearer $DEFAULT_TOKEN".urlEncode()}") &&
                        content.contains("redirect_to=${url.urlEncode()}")
                }
            )
        }

    @Test
    fun `given Jetpack SSO authentication, when authenticating and loading a URL, then it should authenticate using SSO`() =
        testBlocking {
            givenWPComCredentials()
            givenSite {
                url = "https://example.com"
            }
            val webView = mock<WebView>()
            val webViewEvents = MutableSharedFlow<WCWebViewEvent>()
            val url = "https://example.com/test"
            givenAuthenticationFlow(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.JetpackSSO)

            launch {
                sut.authenticateAndLoadUrl(webView, url, webViewEvents)
            }
            webViewEvents.emit(WCWebViewEvent.PageFinished(WebViewAuthenticator.JETPACK_SSO_TEMP_REDIRECT_URL))

            verify(webView).postUrl(eq(WebViewAuthenticator.WPCOM_LOGIN_URL), any())
            verify(webViewCookieManager).setCookie("https://example.com", "jetpack_sso_redirect_to=$url")
            verify(webView).loadUrl("${selectedSite.get().loginUrlOrDefault}?action=jetpack-sso")
        }

    @Test
    fun `given site credentials authentication, when authenticating and loading a URL, then it should authenticate using site credentials`() =
        testBlocking {
            givenSite {
                url = "https://example.com"
                username = DEFAULT_USERNAME
                password = DEFAULT_PASSWORD
            }
            val webView = mock<WebView>()
            val url = "https://example.com/test"
            givenAuthenticationFlow(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.SiteCredentials)

            sut.authenticateAndLoadUrl(webView, url, emptyFlow())

            verify(webView).postUrl(
                eq(selectedSite.get().loginUrlOrDefault),
                argThat {
                    val content = toString(Charsets.UTF_8)
                    content.contains("log=$DEFAULT_USERNAME") &&
                        content.contains("pwd=$DEFAULT_PASSWORD") &&
                        content.contains("redirect_to=${url.urlEncode()}")
                }
            )
        }

    @Test
    fun `given site credentials authentication, when authentication fails, then it should load the original URL`() =
        testBlocking {
            givenSite {
                url = "https://example.com"
                username = DEFAULT_USERNAME
                password = DEFAULT_PASSWORD
            }
            val webView = mock<WebView>()
            val webViewEvents = MutableSharedFlow<WCWebViewEvent>()
            val url = "https://example.com/test"
            givenAuthenticationFlow(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.SiteCredentials)

            launch {
                sut.authenticateAndLoadUrl(webView, url, webViewEvents)
            }
            webViewEvents.emit(WCWebViewEvent.UrlFailed(selectedSite.get().loginUrlOrDefault, 404))

            verify(webView).loadUrl(url)
        }

    private fun givenAuthenticationFlow(flow: WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow) {
        given(authenticationFlowResolver.resolve(any())).willReturn(flow)
    }

    private fun givenWPComCredentials() {
        given(accountStore.accessToken).willReturn(DEFAULT_TOKEN)
        given(accountStore.account).willReturn(AccountModel().apply { userName = DEFAULT_USERNAME })
    }

    private fun givenSite(siteBuilder: SiteModel.() -> Unit) {
        given(selectedSite.get()).willReturn(SiteModel().apply(siteBuilder))
    }

    companion object {
        private const val DEFAULT_TOKEN = "token"
        private const val DEFAULT_USERNAME = "username"
        private const val DEFAULT_PASSWORD = "password"
    }
}
