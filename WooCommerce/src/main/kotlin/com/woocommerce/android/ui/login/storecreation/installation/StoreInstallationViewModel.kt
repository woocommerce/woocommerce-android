package com.woocommerce.android.ui.login.storecreation.installation

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.installation.ObserveSiteInstallation.InstallationState
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.StoreCreationLoadingState
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.SuccessState
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class StoreInstallationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StoreCreationRepository,
    private val newStore: NewStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val storeCreationLoadingTimer: StoreCreationLoadingTimer,
    private val installationTransactionLauncher: InstallationTransactionLauncher,
    private val observeSiteInstallation: ObserveSiteInstallation,
) : ScopedViewModel(savedStateHandle) {

    private val newStoreUrl
        get() = selectedSite.get().url

    private val newStoreWpAdminUrl
        get() = newStoreUrl.slashJoin("wp-admin/")

    private val _viewState = savedState.getStateFlow<ViewState>(
        this,
        StoreCreationLoadingState(
            progress = 0F,
            title = string.store_creation_in_progress_title_1,
            description = string.store_creation_in_progress_description_1
        )
    )

    val viewState = _viewState
        .onEach {
            if (it is SuccessState && FeatureFlag.FREE_TRIAL_M2.isEnabled()) {
                triggerEvent(NavigateToNewStore)
            }
        }.asLiveData()

    val performanceObserver: LifecycleObserver = installationTransactionLauncher

    private var hasReportedDesync = false

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_INSTALLATION
            )
        )
        loadNewStore()
    }

    private suspend fun processStoreInstallationState(result: InstallationState) {
        when (result) {
            is InstallationState.Success -> {
                repository.selectSite(newStore.data.siteId!!)

                val properties = mapOf(
                    AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource(),
                    AnalyticsTracker.KEY_URL to newStore.data.domain!!,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                    AnalyticsTracker.KEY_IS_FREE_TRIAL to FeatureFlag.FREE_TRIAL_M2.isEnabled()
                )
                installationTransactionLauncher.onStoreInstalled(properties)

                _viewState.update { SuccessState(newStoreWpAdminUrl) }
            }

            is InstallationState.Failure -> {
                installationTransactionLauncher.onStoreInstallationFailed()
                if (result.type == StoreCreationErrorType.STORE_NOT_READY) {
                    analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_TIMED_OUT)
                } else {
                    analyticsTrackerWrapper.track(
                        AnalyticsEvent.SITE_CREATION_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource(),
                            AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                            AnalyticsTracker.KEY_IS_FREE_TRIAL to FeatureFlag.FREE_TRIAL_M2.isEnabled()
                        )
                    )
                }

                _viewState.update { ErrorState(result.type, result.message) }
            }

            is InstallationState.OutOfSync -> {
                if (!hasReportedDesync) {
                    analyticsTrackerWrapper.track(AnalyticsEvent.SITE_CREATION_PROPERTIES_OUT_OF_SYNC)
                    hasReportedDesync = true
                }
            }

            InstallationState.InProgress -> Unit
        }
    }

    private fun loadNewStore() {
        launch {
            combine(
                observeSiteInstallation.invoke(
                    newStore.data.siteId!!,
                    newStore.data.name.orEmpty()
                ),
                storeCreationLoadingTimer.observe()
            ) { installationState, timerState ->
                processStoreInstallationState(installationState)

                when (installationState) {
                    is InstallationState.Success,
                    is InstallationState.Failure -> {
                        this.cancel()
                        storeCreationLoadingTimer.resetTimer()
                    }

                    else -> _viewState.value = timerState
                }
            }.collect()
        }
        storeCreationLoadingTimer.startTimer()
        installationTransactionLauncher.onStoreInstallationRequested()
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

    sealed interface ViewState : Parcelable {
        @Parcelize
        data class StoreCreationLoadingState(
            val progress: Float,
            @StringRes val title: Int,
            @StringRes val description: Int
        ) : ViewState

        @Parcelize
        data class ErrorState(val errorType: StoreCreationErrorType, val message: String? = null) :
            ViewState

        @Parcelize
        data class SuccessState(val url: String) : ViewState
    }

    data class OpenStore(val url: String) : Event()
    object NavigateToNewStore : Event()
}
