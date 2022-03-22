package com.woocommerce.android.ui.common.wpcomwebview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentWpcomWebviewBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.StringUtils
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

private const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"

/**
 * This fragments allows loading specific pages from WordPress.com with the current user logged in.
 * It accepts two parameters:
 * urlToLoad: the initial URL to load
 * urlToTriggerExit: optional URL or part of URL to trigger exit with notice when loaded.
 */
@AndroidEntryPoint
class WPComWebViewFragment : BaseFragment(R.layout.fragment_wpcom_webview), UrlInterceptor {
    companion object {
        const val WEBVIEW_RESULT = "webview-result"
    }

    private val webViewClient by lazy { WPComWebViewClient(this) }
    private val navArgs: WPComWebViewFragmentArgs by navArgs()

    @Inject lateinit var accountStore: AccountStore

    @Inject lateinit var userAgent: UserAgent

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWpcomWebviewBinding.bind(view)
        with(binding.webView) {
            this.webViewClient = this@WPComWebViewFragment.webViewClient
            this.webChromeClient = object : WebChromeClient() {
                @Suppress("MagicNumber")
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.isVisible = progress != 100
                    binding.progressBar.progress = progress
                }
            }
            this.settings.javaScriptEnabled = true
            settings.userAgentString = userAgent.getUserAgent()
        }

        loadAuthenticatedUrl(binding.webView, navArgs.urlToLoad)
    }

    private fun loadAuthenticatedUrl(webView: WebView, urlToLoad: String) {
        val postData = getAuthenticationPostData(
            urlToLoad = urlToLoad,
            username = accountStore.account.userName,
            token = accountStore.accessToken
        )

        webView.postUrl(WPCOM_LOGIN_URL, postData.toByteArray())
    }

    override fun onLoadUrl(url: String) {
        navArgs.urlToTriggerExit?.let {
            if (isAdded && url.contains(it)) {
                navigateBackWithNotice(WEBVIEW_RESULT)
            }
        }
    }

    fun getAuthenticationPostData(urlToLoad: String, username: String, token: String): String {
        val utf8 = StandardCharsets.UTF_8.name()
        try {
            var postData = String.format(
                "log=%s&redirect_to=%s",
                URLEncoder.encode(StringUtils.notNullStr(username), utf8),
                URLEncoder.encode(StringUtils.notNullStr(urlToLoad), utf8)
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
