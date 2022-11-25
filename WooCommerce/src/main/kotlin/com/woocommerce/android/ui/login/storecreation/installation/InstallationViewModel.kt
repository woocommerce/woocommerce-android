package com.woocommerce.android.ui.login.storecreation.installation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.CheckoutState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState.Error
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState.Error.PLAN_PURCHASE_FAILED
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState.Error.SITE_CREATION_FAILED
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState.Error.STORE_LOADING_FAILED
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.InitializationState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.SuccessState
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class InstallationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    private val repository: StoreCreationRepository,
    private val newStore: NewStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val NEW_SITE_LANGUAGE_ID = "en"
        private const val NEW_SITE_THEME = "pub/zoologist"
        private const val CART_URL = "https://wordpress.com/checkout"
        private const val WEBVIEW_SUCCESS_TRIGGER_KEYWORD = "https://wordpress.com/checkout/thank-you/"
        private const val WEBVIEW_EXIT_TRIGGER_KEYWORD = "https://woocommerce.com/"
        private const val STORE_LOAD_RETRIES_LIMIT = 10
        private const val INITIAL_STORE_CREATION_DELAY = 40000L
        private const val SITE_CHECK_DEBOUNCE = 5000L
    }

    private val _viewState = savedState.getStateFlow<ViewState>(this, InitializationState)
    val viewState = _viewState
        .onEach {
            if (it is InitializationState) {
                launchInstallation()
            }
        }.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_INSTALLATION
            )
        )
    }

    private fun launchInstallation() {
        suspend fun <T : Any?> handleErrorOrProceed(
            result: WooResult<T>,
            error: Error,
            successAction: suspend () -> Unit
        ) {
            when {
                result.isError -> {
                    _viewState.update { ErrorState(error, result.error.message) }
                }
                result.model == null -> _viewState.update { ErrorState(error) }
                else -> successAction()
            }
        }

        launch {
            val siteCreationResult = createSite()
            handleErrorOrProceed(siteCreationResult, SITE_CREATION_FAILED) {
                val cartCreationResult = repository.addPlanToCart(newStore.data.siteId!!)
                handleErrorOrProceed(cartCreationResult, PLAN_PURCHASE_FAILED) {
                    showCheckoutWebsite()
                }
            }
        }
    }

    private fun showCheckoutWebsite() {
        _viewState.update {
            CheckoutState(
                startUrl = "$CART_URL/${newStore.data.domain}",
                successTriggerKeyword = WEBVIEW_SUCCESS_TRIGGER_KEYWORD,
                exitTriggerKeyword = WEBVIEW_EXIT_TRIGGER_KEYWORD
            )
        }
    }

    private suspend fun createSite(): WooResult<Long> {
        val result = repository.createNewSite(
            SiteCreationData(
                siteDesign = NEW_SITE_THEME,
                domain = newStore.data.domain,
                title = newStore.data.name,
                segmentId = null
            ),
            NEW_SITE_LANGUAGE_ID,
            TimeZone.getDefault().id
        )

        if (!result.isError) {
            newStore.update(siteId = result.model)
        }

        return result
    }

    private fun onStoreCreationFinished(isStoreReady: Boolean) {
        if (isStoreReady) {
            repository.selectSite(newStore.data.siteId!!)

            val properties = mapOf(
                AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource(),
                AnalyticsTracker.KEY_URL to newStore.data.domain!!,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE
            )
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SITE_CREATED, properties)

            _viewState.update { SuccessState("https://${newStore.data.domain!!}") }
        } else {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.SITE_CREATION_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource(),
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE
                )
            )

            _viewState.update { ErrorState(STORE_LOADING_FAILED) }
        }
    }

    fun onShowPreviewButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_SITE_PREVIEWED)
        triggerEvent(OpenStore("https://${newStore.data.domain!!}"))
    }

    fun onManageStoreButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_STORE_MANAGEMENT_OPENED)
        triggerEvent(NavigateToNewStore)
    }

    fun onRetryButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_SITE_LOADING_RETRIED)
        onStoreCreated()
    }

    fun onStoreCreated() {
        launch {
            _viewState.update { LoadingState(true) }

            // it takes a while (~45s) before a store is ready after a purchase, so we need to wait a bit
            delay(INITIAL_STORE_CREATION_DELAY)

            // keep fetching the user's sites until the new site is properly configured or the retry limit is reached
            for (retries in 1..STORE_LOAD_RETRIES_LIMIT) {
                val result = repository.fetchSiteAfterCreation(newStore.data.siteId!!)
                val isStoreReady = result.getOrNull() == true
                if (isStoreReady || result.isFailure) {
                    onStoreCreationFinished(isStoreReady)
                    break
                } else if (retries == STORE_LOAD_RETRIES_LIMIT) {
                    WooLog.d(T.LOGIN, "Max number of store load retries reached...")
                    onStoreCreationFinished(false)
                }
                delay(SITE_CHECK_DEBOUNCE)
            }
        }
    }

    fun onExitTriggered() {
        triggerEvent(Exit)
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        object InitializationState : ViewState

        @Parcelize
        data class LoadingState(val isMessageShown: Boolean = false) : ViewState

        @Parcelize
        data class ErrorState(val error: Error, val message: String? = null) : ViewState {
            enum class Error {
                SITE_CREATION_FAILED,
                PLAN_PURCHASE_FAILED,
                STORE_LOADING_FAILED
            }
        }

        @Parcelize
        data class SuccessState(val url: String) : ViewState

        @Parcelize
        data class CheckoutState(
            val startUrl: String,
            val successTriggerKeyword: String,
            val exitTriggerKeyword: String
        ) : ViewState
    }

    data class OpenStore(val url: String) : Event()
    object NavigateToNewStore : Event()
}
