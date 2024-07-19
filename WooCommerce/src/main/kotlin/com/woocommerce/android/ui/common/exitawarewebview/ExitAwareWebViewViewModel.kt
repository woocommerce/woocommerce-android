package com.woocommerce.android.ui.common.exitawarewebview

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.common.SharedWebViewFlow
import com.woocommerce.android.ui.common.WebViewEvent
import com.woocommerce.android.ui.common.exitawarewebview.ExitAwareWebViewViewModel.UrlComparisonMode.EQUALITY
import com.woocommerce.android.ui.common.exitawarewebview.ExitAwareWebViewViewModel.UrlComparisonMode.PARTIAL
import com.woocommerce.android.ui.common.exitawarewebview.ExitAwareWebViewViewModel.UrlComparisonMode.STARTS_WITH
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class ExitAwareWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val userAgent: UserAgent,
    private val sharedWebViewFlow: SharedWebViewFlow
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: ExitAwareWebViewFragmentArgs by savedStateHandle.navArgs()
    private var isExiting = false

    // `WebView`'s onPageFinished callback is called multiple times when loading the same URL once,
    // so this flag is used to ensure that the event is only emitted once.
    private var isUrlToLoadFinishedOnce = false

    // `WebView`s onPageFinished callback is still called even if the url failed to load.
    // This flag is used to ensure it is not emitted on failure.
    private var isUrlLoadingFailed = false

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
            launch {
                sharedWebViewFlow.emitEvent(WebViewEvent.onTriggerUrlLoaded(url))
            }
        }
    }

    fun onPageFinished(url: String) {
        if (isUrlLoadingFailed) {
            isUrlLoadingFailed = false
            return
        }

        if (url == viewState.urlToLoad && !isUrlToLoadFinishedOnce) {
            isUrlToLoadFinishedOnce = true
            launch {
                sharedWebViewFlow.emitEvent(WebViewEvent.onPageFinished(url))
            }
        }
    }

    fun onClose() {
        launch {
            sharedWebViewFlow.emitEvent(WebViewEvent.onWebViewClosed)
        }
        triggerEvent(Exit)
    }

    fun onUrlFailed(url: String, errorCode: Int?) {
        isUrlLoadingFailed = true
        launch {
            sharedWebViewFlow.emitEvent(WebViewEvent.onUrlFailed(url, errorCode))
        }
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
