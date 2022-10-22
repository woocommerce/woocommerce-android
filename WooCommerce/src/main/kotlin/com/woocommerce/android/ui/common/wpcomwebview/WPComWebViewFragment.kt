package com.woocommerce.android.ui.common.wpcomwebview

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentWpcomWebviewBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment.UrlComparisonMode.EQUALITY
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment.UrlComparisonMode.PARTIAL
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

/**
 * This fragments allows loading specific pages from WordPress.com with the current user logged in.
 * It accepts two parameters:
 * urlToLoad: the initial URL to load
 * urlToTriggerExit: optional URL or part of URL to trigger exit with notice when loaded.
 */
@AndroidEntryPoint
class WPComWebViewFragment : BaseFragment(R.layout.fragment_wpcom_webview), UrlInterceptor, BackPressListener {
    companion object {
        const val WEBVIEW_RESULT = "webview-result"
        const val WEBVIEW_RESULT_WITH_URL = "webview-result-with-url"
        const val WEBVIEW_DISMISSED = "webview-dismissed"
        const val WEBVIEW_STORE_CHECKOUT_STRING = "checkout/thank-you/"
        const val WEBVIEW_STORE_URL_KEY = "store-url-key"
    }

    private val webViewClient by lazy { WPComWebViewClient(this) }
    private val navArgs: WPComWebViewFragmentArgs by navArgs()
    private var siteUrls = ArrayList<String>()

    @Inject lateinit var wpcomWebViewAuthenticator: WPComWebViewAuthenticator

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
            this.settings.domStorageEnabled = true
            settings.userAgentString = userAgent.userAgent
        }

        siteUrls = savedInstanceState?.getStringArrayList(WEBVIEW_STORE_URL_KEY) ?: siteUrls

        wpcomWebViewAuthenticator.authenticateAndLoadUrl(binding.webView, navArgs.urlToLoad)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(WEBVIEW_STORE_URL_KEY, siteUrls)
        super.onSaveInstanceState(outState)
    }

    override fun onLoadUrl(url: String) {
        fun String.matchesUrl(url: String) = when (navArgs.urlComparisonMode) {
            PARTIAL -> url.contains(this, ignoreCase = true)
            EQUALITY -> equals(url, ignoreCase = true)
        }

        Log.d("Webview", url)
        extractSiteUrl(url)

        if (isAdded && navArgs.urlToTriggerExit?.matchesUrl(url) == true) {
            if (siteUrls.isEmpty()) {
                navigateBackWithNotice(WEBVIEW_RESULT)
            } else {
                navigateBackWithResult(WEBVIEW_RESULT_WITH_URL, siteUrls)
            }
        }
    }

    private fun extractSiteUrl(url: String) {
        "$WEBVIEW_STORE_CHECKOUT_STRING.+/".toRegex().find(url)?.range?.let { range ->
            val start = range.first + WEBVIEW_STORE_CHECKOUT_STRING.length
            val end = range.last
            val siteUrl = url.substring(start, end)
            if (!siteUrls.contains(siteUrl)) {
                siteUrls.add(siteUrl)
            }
        }
    }

    override fun getFragmentTitle() = navArgs.title ?: super.getFragmentTitle()

    override fun onRequestAllowBackPress(): Boolean {
        navigateBackWithNotice(WEBVIEW_DISMISSED)
        return false
    }

    enum class UrlComparisonMode {
        PARTIAL, EQUALITY
    }
}
