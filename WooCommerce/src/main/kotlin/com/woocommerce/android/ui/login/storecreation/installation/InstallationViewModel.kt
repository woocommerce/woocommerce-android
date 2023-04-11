package com.woocommerce.android.ui.login.storecreation.installation

import android.os.Parcelable
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.STORE_LOADING_FAILED
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Failure
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult.Success
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.InitialState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.installation.InstallationViewModel.ViewState.SuccessState
import com.woocommerce.android.util.FeatureFlag
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
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class InstallationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StoreCreationRepository,
    private val newStore: NewStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val installationTransactionLauncher: InstallationTransactionLauncher,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val STORE_LOAD_RETRIES_LIMIT = 10
        private const val INITIAL_STORE_CREATION_DELAY = 40000L
        private const val SITE_CHECK_DEBOUNCE = 5000L
    }

    private val newStoreUrl
        get() = selectedSite.get().url

    private val newStoreWpAdminUrl
        get() = newStoreUrl.slashJoin("wp-admin/")

    private val _viewState = savedState.getStateFlow<ViewState>(this, InitialState)
    val viewState = _viewState
        .onEach {
            if (it is InitialState) {
                loadNewStore()
            } else if (it is SuccessState && FeatureFlag.FREE_TRIAL_M2.isEnabled()) {
                triggerEvent(NavigateToNewStore)
            }
        }.asLiveData()

    val performanceObserver: LifecycleObserver = installationTransactionLauncher

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_INSTALLATION
            )
        )
    }

    private fun loadNewStore() {
        suspend fun processStoreCreationResult(result: StoreCreationResult<Unit>) {
            if (result is Success) {
                installationTransactionLauncher.onStoreInstalled()
                repository.selectSite(newStore.data.siteId!!)

                val properties = mapOf(
                    AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource(),
                    AnalyticsTracker.KEY_URL to newStore.data.domain!!,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                    AnalyticsTracker.KEY_IS_FREE_TRIAL to FeatureFlag.FREE_TRIAL_M2.isEnabled()
                )
                analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_WOOCOMMERCE_SITE_CREATED, properties)

                _viewState.update { SuccessState(newStoreWpAdminUrl) }
            } else {
                installationTransactionLauncher.onStoreInstallationFailed()
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.SITE_CREATION_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource(),
                        AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                        AnalyticsTracker.KEY_IS_FREE_TRIAL to FeatureFlag.FREE_TRIAL_M2.isEnabled()
                    )
                )

                val error = result as Failure
                _viewState.update { ErrorState(error.type, error.message) }
                newStore.clear()
            }
        }

        launch {
            installationTransactionLauncher.onStoreInstallationRequested()
            _viewState.update { LoadingState }

            // it takes a while (~45s) before a store is ready after a purchase, so we need to wait a bit
            delay(INITIAL_STORE_CREATION_DELAY)

            // keep fetching the user's sites until the new site is properly configured or the retry limit is reached
            for (retries in 1..STORE_LOAD_RETRIES_LIMIT) {
                val result = repository.fetchSiteAfterCreation(newStore.data.siteId!!)
                if (result is Success || // Woo store is ready
                    (result as Failure).type == STORE_LOADING_FAILED || // permanent error
                    retries == STORE_LOAD_RETRIES_LIMIT // site found but is not ready & retry limit reached
                ) {
                    processStoreCreationResult(result)
                    break
                }

                delay(SITE_CHECK_DEBOUNCE)
            }
        }
    }

    fun onUrlLoaded(url: String) {
        if (url.contains(newStoreWpAdminUrl)) {
            _viewState.update { SuccessState(newStoreUrl) }
        }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onShowPreviewButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_SITE_PREVIEWED)
        triggerEvent(OpenStore(newStoreUrl))
    }

    fun onManageStoreButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_STORE_MANAGEMENT_OPENED)
        triggerEvent(NavigateToNewStore)
        newStore.clear()
    }

    fun onRetryButtonClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_SITE_LOADING_RETRIED)
        loadNewStore()
    }

    override fun onCleared() {
        installationTransactionLauncher.clear()
    }

    sealed interface ViewState : Parcelable {
        @Parcelize
        object InitialState : ViewState

        @Parcelize
        object LoadingState : ViewState

        @Parcelize
        data class ErrorState(val errorType: StoreCreationErrorType, val message: String? = null) : ViewState

        @Parcelize
        data class SuccessState(val url: String) : ViewState
    }

    data class OpenStore(val url: String) : Event()
    object NavigateToNewStore : Event()
}
