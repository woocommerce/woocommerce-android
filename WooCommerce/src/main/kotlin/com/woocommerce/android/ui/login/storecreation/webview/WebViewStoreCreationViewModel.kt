package com.woocommerce.android.ui.login.storecreation.webview

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.StoreLoadResult.ERROR
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.StoreLoadResult.STORE_FOUND
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.StoreLoadResult.STORE_NOT_FOUND
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState.StoreCreationState
import com.woocommerce.android.ui.login.storecreation.webview.WebViewStoreCreationViewModel.ViewState.StoreLoadingState
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class WebViewStoreCreationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    private val repository: StoreCreationRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val STORE_CREATION_URL = "https://woocommerce.com/start"
        private const val SITE_URL_KEYWORD = "checkout/thank-you/"
        private const val WEBVIEW_SUCCESS_TRIGGER_KEYWORD = "calypso/images/wpcom-ecommerce"
        private const val WEBVIEW_EXIT_TRIGGER_KEYWORD = "https://woocommerce.com/"
        private const val STORE_LOAD_RETRIES_LIMIT = 10
    }

    private val _dialogViewState = savedStateHandle.getStateFlow(viewModelScope, setDialogState(isVisible = false))
    val dialogViewState = _dialogViewState.asStateFlow().asLiveData()

    private val step = savedStateHandle.getStateFlow<Step>(viewModelScope, Step.StoreCreation)
    val viewState: LiveData<ViewState> = step.map { step ->
        when (step) {
            Step.StoreCreation -> prepareStoreCreationState()
            Step.StoreLoading -> StoreLoadingState
            Step.StoreLoadingError -> prepareErrorState()
        }
    }.asLiveData()

    private val possibleStoreUrls = mutableListOf<String>()
    private val navigationSource by lazy {
        appPrefsWrapper.getStoreCreationSource()
    }

    private fun prepareStoreCreationState() = StoreCreationState(
        storeCreationUrl = STORE_CREATION_URL,
        siteUrlKeyword = SITE_URL_KEYWORD,
        successTriggerKeyword = WEBVIEW_SUCCESS_TRIGGER_KEYWORD,
        exitTriggerKeyword = WEBVIEW_EXIT_TRIGGER_KEYWORD,
        onStoreCreated = ::onStoreCreated,
        onSiteAddressFound = ::onSiteAddressFound,
        onExitTriggered = ::onExitTriggered
    )

    private fun prepareErrorState() = ErrorState(
        onRetryButtonClick = ::onStoreCreated
    )

    private fun setDialogState(isVisible: Boolean) = DialogState(
        isDialogVisible = isVisible
    )

    private suspend fun getOrFetchNewSite(): StoreLoadResult {
        val newSite = possibleStoreUrls.firstNotNullOfOrNull { url -> repository.getSiteBySiteUrl(url) }
        return if (newSite?.isJetpackInstalled == true && newSite.isJetpackConnected) {
            WooLog.d(T.LOGIN, "Found new site: ${newSite.url}")
            repository.selectSite(newSite)
            triggerEvent(NavigateToNewStore)

            val args = mapOf<String, String>(
                AnalyticsTracker.KEY_SOURCE to navigationSource,
                AnalyticsTracker.KEY_URL to newSite.url
            )
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SITE_CREATED, args)
            STORE_FOUND
        } else {
            WooLog.d(T.LOGIN, "New site not found, retrying...")
            val result = repository.fetchSitesAfterCreation()
            return if (result.isFailure) {
                step.value = Step.StoreLoadingError

                analyticsTrackerWrapper.track(
                    AnalyticsEvent.SITE_CREATION_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_SOURCE to navigationSource
                    )
                )
                ERROR
            } else {
                STORE_NOT_FOUND
            }
        }
    }

    private fun onStoreCreated() {
        step.value = Step.StoreLoading
        launch {
            // keep fetching the user's sites until the new site is properly configured or the retry limit is reached
            for (retries in 1..STORE_LOAD_RETRIES_LIMIT) {
                val result = getOrFetchNewSite()
                if (result == STORE_FOUND || result == ERROR) {
                    break
                } else if (retries == STORE_LOAD_RETRIES_LIMIT) {
                    WooLog.d(T.LOGIN, "Max number of store load retries reached...")
                    step.value = Step.StoreLoadingError
                }
            }
        }
        WooLog.d(T.LOGIN, "Store creation done...")
    }

    private fun onSiteAddressFound(url: String) {
        possibleStoreUrls.add(url)
    }

    fun onDialogDismissed() {
        _dialogViewState.value = setDialogState(isVisible = false)
    }

    fun onExitTriggered() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to navigationSource
            )
        )

        _dialogViewState.value = setDialogState(isVisible = false)
        triggerEvent(Exit)
    }

    fun onBackPressed() {
        _dialogViewState.value = setDialogState(isVisible = true)
    }

    sealed interface ViewState {
        data class StoreCreationState(
            val storeCreationUrl: String,
            val siteUrlKeyword: String,
            val successTriggerKeyword: String,
            val exitTriggerKeyword: String,
            val onStoreCreated: () -> Unit,
            val onExitTriggered: () -> Unit,
            val onSiteAddressFound: (url: String) -> Unit
        ) : ViewState

        object StoreLoadingState : ViewState

        data class ErrorState(
            val onRetryButtonClick: () -> Unit
        ) : ViewState
    }

    @Parcelize
    data class DialogState(
        val isDialogVisible: Boolean
    ) : Parcelable

    object NavigateToNewStore : MultiLiveEvent.Event()

    private sealed interface Step : Parcelable {
        @Parcelize
        object StoreCreation : Step

        @Parcelize
        object StoreLoading : Step

        @Parcelize
        object StoreLoadingError : Step
    }

    private enum class StoreLoadResult {
        STORE_FOUND, STORE_NOT_FOUND, ERROR
    }
}
