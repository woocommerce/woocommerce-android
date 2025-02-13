package com.woocommerce.android.ui.common.webview

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import com.woocommerce.android.extensions.loginUrlOrDefault
import com.woocommerce.android.extensions.urlEncode
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.compose.component.web.WCWebViewEvent
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.wordpress.android.fluxc.store.AccountStore
import java.io.UnsupportedEncodingException
import javax.inject.Inject

class WebViewAuthenticator @Inject constructor(
    private val authenticationFlowResolver: WebViewAuthenticationFlowResolver,
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val webViewCookieManager: CookieManager
) {
    suspend fun authenticateAndLoadUrl(webView: WebView, url: String, webViewEvents: Flow<WCWebViewEvent>) {
        val authenticationFlow = authenticationFlowResolver.resolve(url)
        when (authenticationFlow) {
            WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.WPCom -> {
                authenticateWPComAndLoad(webView, url)
            }

            WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.JetpackSSO -> {
                authenticateSSOAndLoad(webView, url, webViewEvents)
            }

            WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.SiteCredentials -> {
                authenticateUsingSiteCredentialsAndLoad(webView, url, webViewEvents)
            }

            WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.None -> {
                webView.loadUrl(url)
            }
        }
    }

    private fun authenticateWPComAndLoad(webView: WebView, url: String): Boolean {
        val postData = prepareLoginPostData(
            redirectUrl = url,
            username = accountStore.account.userName,
            authorizationParam = "authorization" to "Bearer ${accountStore.accessToken}"
        )

        if (postData != null) {
            webView.postUrl(WPCOM_LOGIN_URL, postData.toByteArray())
            return true
        } else {
            webView.loadUrl(url)
            return false
        }
    }

    private suspend fun authenticateSSOAndLoad(webView: WebView, url: String, webViewEvents: Flow<WCWebViewEvent>) {
        authenticateWPComAndLoad(webView, JETPACK_SSO_TEMP_REDIRECT_URL).also {
            if (!it) {
                // The authentication failed, so load the original URL
                webView.loadUrl(url)
                return
            }
        }

        // Wait for the WPCom login to complete
        webViewEvents.first { it is WCWebViewEvent.PageFinished && it.url == JETPACK_SSO_TEMP_REDIRECT_URL }

        // Handle SSO login and redirect back to the original URL
        val site = selectedSite.get()
        webViewCookieManager.setCookie(site.url, "jetpack_sso_redirect_to=$url")
        val ssoLoginUrl = site.loginUrlOrDefault.toHttpUrl().newBuilder()
            .addQueryParameter("action", "jetpack-sso")
            .build()
            .toString()

        webView.loadUrl(ssoLoginUrl)
    }

    private suspend fun authenticateUsingSiteCredentialsAndLoad(
        webView: WebView,
        url: String,
        webViewEvents: Flow<WCWebViewEvent>
    ) {
        val site = selectedSite.get()

        val postData = prepareLoginPostData(
            redirectUrl = url,
            username = site.username,
            authorizationParam = "pwd" to site.password
        )

        if (postData != null) {
            webView.postUrl(site.loginUrlOrDefault, postData.toByteArray())
        } else {
            webView.loadUrl(url)
        }

        val event = webViewEvents.firstOrNull { it is WCWebViewEvent.PageFinished || it is WCWebViewEvent.UrlFailed }
        if (event is WCWebViewEvent.UrlFailed && event.url == site.loginUrlOrDefault) {
            // In case we failed to authenticate, load the original URL
            // The failure could happen if  some other security measures were added that would prevent
            // native handling of login (like using a custom login page or a captcha)
            WooLog.w(WooLog.T.UTILS, "Failed to authenticate the WebView using site credentials, load the original URL")
            webView.loadUrl(url)
        }
    }

    private fun prepareLoginPostData(
        redirectUrl: String,
        username: String,
        authorizationParam: Pair<String, String>,
    ): String? {
        val (authorizationKey, authorizationValue) = authorizationParam
        return try {
            buildString {
                append("redirect_to=").append(redirectUrl.urlEncode())

                append("&log=").append(username.urlEncode())

                append("&${authorizationKey.urlEncode()}=")
                    .append(authorizationValue.urlEncode())
            }
        } catch (e: UnsupportedEncodingException) {
            WooLog.e(WooLog.T.UTILS, e)
            null
        }
    }

    companion object {
        @VisibleForTesting
        const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"
        const val JETPACK_SSO_TEMP_REDIRECT_URL = "https://wordpress.com/mobile-redirect"
    }
}
