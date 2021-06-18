package com.woocommerce.android.ui.common.wpcomwebview

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentWpcomWebviewBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.StringUtils
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

private const val WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php"

@AndroidEntryPoint
class WPComWebViewFragment : BaseFragment(R.layout.fragment_wpcom_webview), UrlIntercepter {
    private val webViewClient by lazy { WPComWebViewClient(this) }
    private val navArgs: WPComWebViewFragmentArgs by navArgs()

    @Inject
    lateinit var accountStore: AccountStore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val webView = FragmentWpcomWebviewBinding.bind(view).root
        webView.webViewClient = webViewClient
        authenticateUser(webView)
    }

    private fun authenticateUser(webView: WebView) {
        val postData = getAuthenticationPostData(
            urlToLoad = navArgs.url,
            username = accountStore.account.userName,
            token = accountStore.accessToken
        )

        webView.postUrl(WPCOM_LOGIN_URL, postData.toByteArray())
    }

    override fun onLoadUrl(url: String) {
        println(url)
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