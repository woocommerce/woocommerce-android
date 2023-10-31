package com.woocommerce.android.ui.login.storecreation.installation

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
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
    private val savedStateHandle: SavedStateHandle,
    private val repository: StoreCreationRepository,
    private val newStore: NewStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val storeInstallationLoadingTimer: StoreInstallationLoadingTimer,
    private val installationTransactionLauncher: InstallationTransactionLauncher,
    private val observeSiteInstallation: ObserveSiteInstallation
) : ScopedViewModel(savedStateHandle) {

    private val newStoreUrl
        get() = selectedSite.get().url

    private val newStoreWpAdminUrl
        get() = newStoreUrl.slashJoin("wp-admin/")

    private var storeData
        get() = savedStateHandle.get<NewStore.NewStoreData>(STORE_DATA_KEY)!!
        set(value) {
            savedStateHandle[STORE_DATA_KEY] = value
        }

    private val _viewState = savedState.getStateFlow<ViewState>(
        this,
        StoreCreationLoadingState(
            progress = 0F,
            title = R.string.store_creation_in_progress_title_1,
            description = R.string.store_creation_in_progress_description_1,
            image = R.drawable.store_creation_loading_almost_there
        )
    )

    val viewState = _viewState
        .onEach {
            if (it is SuccessState) {
                triggerEvent(NavigateToNewStore)
            }
        }.asLiveData()

    val performanceObserver: LifecycleObserver = installationTransactionLauncher

    private var hasReportedDesync = false

    init {
        if (!savedStateHandle.contains(STORE_DATA_KEY)) {
            storeData = newStore.data
        }

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
                repository.selectSite(storeData.siteId!!)

                val properties = mapOf(
                    AnalyticsTracker.KEY_SOURCE to appPrefsWrapper.getStoreCreationSource(),
                    AnalyticsTracker.KEY_URL to storeData.domain!!,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                    AnalyticsTracker.KEY_IS_FREE_TRIAL to true
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
                            AnalyticsTracker.KEY_IS_FREE_TRIAL to true
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
                    storeData.siteId!!,
                    storeData.name.orEmpty()
                ),
                storeInstallationLoadingTimer.observe()
            ) { installationState, timerState ->
                processStoreInstallationState(installationState)

                when (installationState) {
                    is InstallationState.Success,
                    is InstallationState.Failure -> {
                        this.cancel()
                        storeInstallationLoadingTimer.resetTimer()
                    }

                    else -> _viewState.value = timerState
                }
            }.collect()
        }
        storeInstallationLoadingTimer.startTimer()
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
            @StringRes val description: Int,
            @DrawableRes val image: Int
        ) : ViewState

        @Parcelize
        data class ErrorState(val errorType: StoreCreationErrorType, val message: String? = null) :
            ViewState

        @Parcelize
        data class SuccessState(val url: String) : ViewState
    }

    data class OpenStore(val url: String) : Event()
    object NavigateToNewStore : Event()

    companion object {
        const val TRIAL_LENGTH_IN_DAYS = 14L
        private const val STORE_DATA_KEY = "store_data_key"
    }
}
