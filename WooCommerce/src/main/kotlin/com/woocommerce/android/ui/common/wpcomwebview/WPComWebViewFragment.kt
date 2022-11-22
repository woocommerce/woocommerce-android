package com.woocommerce.android.ui.common.wpcomwebview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment.UrlComparisonMode.EQUALITY
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment.UrlComparisonMode.PARTIAL
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
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
class WPComWebViewFragment : BaseFragment(R.layout.fragment_wpcom_webview) {
    companion object {
        const val WEBVIEW_RESULT = "webview-result"
        const val WEBVIEW_DISMISSED = "webview-dismissed"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val navArgs: WPComWebViewFragmentArgs by navArgs()

    @Inject lateinit var wpcomWebViewAuthenticator: WPComWebViewAuthenticator

    @Inject lateinit var userAgent: UserAgent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    WPComWebViewScreen(
                        navArgs = navArgs,
                        wpcomWebViewAuthenticator = wpcomWebViewAuthenticator,
                        userAgent = userAgent,
                        onUrlLoaded = ::onLoadUrl,
                        onClose = ::onClose
                    )
                }
            }
        }
    }

    private fun onLoadUrl(url: String) {
        fun String.matchesUrl(url: String) = when (navArgs.urlComparisonMode) {
            PARTIAL -> url.contains(this, ignoreCase = true)
            EQUALITY -> equals(url, ignoreCase = true)
        }

        if (isAdded && navArgs.urlToTriggerExit?.matchesUrl(url) == true) {
            navigateBackWithNotice(WEBVIEW_RESULT)
        }
    }

    private fun onClose() {
        navigateBackWithNotice(WEBVIEW_DISMISSED)
    }

    enum class UrlComparisonMode {
        PARTIAL, EQUALITY
    }
}
