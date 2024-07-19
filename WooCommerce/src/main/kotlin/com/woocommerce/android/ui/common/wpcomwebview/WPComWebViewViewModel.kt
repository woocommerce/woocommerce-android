package com.woocommerce.android.ui.common.wpcomwebview

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.common.SharedWebViewFlow
import com.woocommerce.android.ui.common.WebViewEvent
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewViewModel.UrlComparisonMode.EQUALITY
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewViewModel.UrlComparisonMode.PARTIAL
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewViewModel.UrlComparisonMode.STARTS_WITH
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class WPComWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    private val sharedWebViewFlow: SharedWebViewFlow
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: WPComWebViewFragmentArgs by savedStateHandle.navArgs()
    private var isExiting = false

    // `WebView`'s onPageFinished callback is called multiple times when loading the same URL once,
    // so this flag is used to ensure that the event is only emitted once.
    private var isUrlToLoadFinishedOnce = false

    val viewState = navArgs.let {
        ViewState(
            urlToLoad = it.urlToLoad,
            title = it.title,
            displayMode = it.displayMode,
            captureBackButton = it.captureBackButton,
            clearCache = it.clearCache
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
        if (url == viewState.urlToLoad && !isUrlToLoadFinishedOnce) {
            isUrlToLoadFinishedOnce = true
            launch {
                sharedWebViewFlow.emitEvent(WebViewEvent.onPageFinished(url))
            }
        }
    }

    fun onClose() {
        triggerEvent(Exit)
    }

    data class ViewState(
        val urlToLoad: String,
        val title: String?,
        val displayMode: DisplayMode,
        val captureBackButton: Boolean,
        val clearCache: Boolean = false
    )

    enum class UrlComparisonMode {
        PARTIAL, EQUALITY, STARTS_WITH
    }

    enum class DisplayMode {
        REGULAR, MODAL
    }
}
