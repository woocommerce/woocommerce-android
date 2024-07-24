package com.woocommerce.android.ui.google.webview

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.google.CanUseAutoLoginWebview
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel.UrlComparisonMode.EQUALITY
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel.UrlComparisonMode.PARTIAL
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel.UrlComparisonMode.STARTS_WITH
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class GoogleAdsWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val canUseAutoLoginWebview: CanUseAutoLoginWebview,
    val userAgent: UserAgent
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: GoogleAdsWebViewFragmentArgs by savedStateHandle.navArgs()
    private var isExiting = false

    // `WebView`'s onPageFinished callback is called multiple times when loading the same URL once,
    // so this flag is needed for callback to be called exactly one time.
    private var isUrlToLoadFinishedOnce = false

    // `WebView`s onPageFinished callback is still called even if the url failed to load.
    // This flag is used to ensure the callback can return early.
    private var isUrlLoadingFailed = false

    val viewState = navArgs.let {
        ViewState(
            urlToLoad = it.urlToLoad,
            title = it.title,
            displayMode = it.displayMode,
            captureBackButton = it.captureBackButton,
            clearCache = it.clearCache,
            canUseAutoLoginWebview = canUseAutoLoginWebview()
        )
    }

    fun onUrlLoaded(url: String) {
        fun String.matchesUrl(url: String) = when (navArgs.urlComparisonMode) {
            PARTIAL -> url.contains(this, ignoreCase = true)
            EQUALITY -> equals(url, ignoreCase = true)
            STARTS_WITH -> url.startsWith(this, ignoreCase = true)
        }

        if (navArgs.urlsToTriggerExit?.any { it.matchesUrl(url) } == true && !isExiting) {
            isExiting = true
            triggerEvent(ExitWithResult(Unit))
        }
    }

    fun onPageFinished(url: String) {
        if (isUrlLoadingFailed) {
            isUrlLoadingFailed = false
            return
        }

        if (url == viewState.urlToLoad && !isUrlToLoadFinishedOnce) {
            isUrlToLoadFinishedOnce = true
        }
    }

    fun onClose() {
        triggerEvent(Exit)
    }

    fun onUrlFailed(url: String, errorCode: Int?) {
        WooLog.d(WooLog.T.GOOGLE_ADS, "Failed to load URL: $url, errorCode: $errorCode")
        isUrlLoadingFailed = true
    }

    data class ViewState(
        val urlToLoad: String,
        val title: String?,
        val displayMode: DisplayMode,
        val captureBackButton: Boolean,
        val clearCache: Boolean = false,
        val canUseAutoLoginWebview: Boolean
    )

    enum class UrlComparisonMode {
        PARTIAL, EQUALITY, STARTS_WITH
    }

    enum class DisplayMode {
        REGULAR, MODAL
    }
}
