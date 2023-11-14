package com.woocommerce.android.ui.common.wpcomwebview

import android.webkit.WebView
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.store.AccountStore
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.inject.Inject

private const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"

class WPComWebViewAuthenticator @Inject constructor(
    private val accountStore: AccountStore
) {
    fun authenticateAndLoadUrl(webView: WebView, url: String) {
        getAuthPostData(url).let { postData ->
            if (postData.isNotEmpty()) {
                webView.postUrl(WPCOM_LOGIN_URL, postData.toByteArray())
            } else {
                webView.loadUrl(url)
            }
        }
    }

    @Suppress("ReturnCount")
    private fun getAuthPostData(redirectUrl: String): String {
        val username = accountStore.account.userName.takeIf { it.isNotNullOrEmpty() } ?: return ""
        val token = accountStore.accessToken.takeIf { it.isNotNullOrEmpty() } ?: return ""

        val utf8 = StandardCharsets.UTF_8.name()
        try {
            var postData = String.format(
                Locale.ROOT,
                "log=%s&redirect_to=%s",
                URLEncoder.encode(username, utf8),
                URLEncoder.encode(redirectUrl, utf8),
            )

            // Add token authorization
            postData += "&authorization=Bearer " + URLEncoder.encode(token, utf8)

            return postData
        } catch (e: UnsupportedEncodingException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
        return ""
    }
}
