package com.woocommerce.android.ui.dashboard.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.onboarding.AboutYourStoreTaskRes
import com.woocommerce.android.ui.onboarding.AddProductTaskRes
import com.woocommerce.android.ui.onboarding.CustomizeDomainTaskRes
import com.woocommerce.android.ui.onboarding.LaunchStoreTaskRes
import com.woocommerce.android.ui.onboarding.NameYourStoreTaskRes
import com.woocommerce.android.ui.onboarding.NavigateToAboutYourStore
import com.woocommerce.android.ui.onboarding.NavigateToAddProduct
import com.woocommerce.android.ui.onboarding.NavigateToDomains
import com.woocommerce.android.ui.onboarding.NavigateToLaunchStore
import com.woocommerce.android.ui.onboarding.NavigateToOnboardingFullScreen
import com.woocommerce.android.ui.onboarding.NavigateToSetupPayments
import com.woocommerce.android.ui.onboarding.NavigateToSetupWooPayments
import com.woocommerce.android.ui.onboarding.NavigateToSurvey
import com.woocommerce.android.ui.onboarding.OnboardingTaskUi
import com.woocommerce.android.ui.onboarding.SetupPaymentsTaskRes
import com.woocommerce.android.ui.onboarding.SetupWooPaymentsTaskRes
import com.woocommerce.android.ui.onboarding.ShowNameYourStoreDialog
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository
import com.woocommerce.android.ui.onboarding.toOnboardingTaskState
import com.woocommerce.android.ui.onboarding.toTrackingKey
import com.woocommerce.android.ui.products.AddProductNavigator
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DashboardOnboardingViewModel.Factory::class)
class DashboardOnboardingViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val onboardingRepository: StoreOnboardingRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkStatus: NetworkStatus,
    val addProductNavigator: AddProductNavigator
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val MAX_NUMBER_OF_TASK_TO_DISPLAY_IN_CARD = 3
    }

    private val _viewState = MutableLiveData(
        OnboardingDashBoardState(
            title = DashboardWidget.Type.ONBOARDING.titleResource,
            tasks = emptyList(),
            menu = DashboardWidgetMenu(
                items = listOf(
                    DashboardWidgetAction(
                        titleResource = R.string.store_onboarding_menu_share_feedback,
                        action = ::onShareFeedbackClicked
                    ),
                    DashboardWidget.Type.ONBOARDING.defaultHideMenuEntry {
                        parentViewModel.onHideWidgetClicked(DashboardWidget.Type.ONBOARDING)
                    }
                )
            ),
            onViewAllTapped = DashboardWidgetAction(
                titleResource = R.string.store_onboarding_task_view_all_tasks,
                action = ::viewAllClicked
            )
        )
    )
    val viewState = _viewState

    private val refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)

    init {
        launch {
            merge(refreshTrigger, parentViewModel.refreshTrigger)
                .onStart { emit(RefreshEvent()) }
                .collectLatest {
                    if (networkStatus.isConnected()) {
                        _viewState.value = _viewState.value?.copy(isLoading = true, isError = false)
                        if (it.isForced) {
                            // Fetch only if it's a forced refresh, in the other cases, the DashboardRepository will
                            // trigger the initial fetch
                            onboardingRepository.fetchOnboardingTasks()
                        }
                    } else {
                        _viewState.value = _viewState.value?.copy(isLoading = false, isError = true)
                    }
                }
        }

        launch {
            onboardingRepository.observeOnboardingTasks()
                .collectLatest { tasks ->
                    _viewState.value = _viewState.value?.copy(
                        tasks = tasks.map { it.toOnboardingTaskState() },
                        isLoading = false,
                        isError = false
                    )
                }
        }
    }

    private fun viewAllClicked() {
        triggerEvent(NavigateToOnboardingFullScreen)
    }

    private fun onShareFeedbackClicked() {
        triggerEvent(NavigateToSurvey)
    }

    fun onTaskClicked(task: OnboardingTaskUi) {
        when (task.taskUiResources) {
            AboutYourStoreTaskRes -> triggerEvent(NavigateToAboutYourStore)
            AddProductTaskRes -> triggerEvent(NavigateToAddProduct)
            CustomizeDomainTaskRes -> triggerEvent(NavigateToDomains)
            LaunchStoreTaskRes -> triggerEvent(NavigateToLaunchStore)
            NameYourStoreTaskRes -> triggerEvent(ShowNameYourStoreDialog)
            SetupPaymentsTaskRes -> triggerEvent(NavigateToSetupPayments)
            SetupWooPaymentsTaskRes -> triggerEvent(NavigateToSetupWooPayments)
        }
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.STORE_ONBOARDING_TASK_TAPPED,
            properties = mapOf(AnalyticsTracker.ONBOARDING_TASK_KEY to task.toTrackingKey())
        )
    }

    fun onRefresh() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.ONBOARDING.trackingIdentifier
            )
        )
        refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    data class OnboardingDashBoardState(
        @StringRes val title: Int,
        val tasks: List<OnboardingTaskUi>,
        val menu: DashboardWidgetMenu,
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val onViewAllTapped: DashboardWidgetAction
    )

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardOnboardingViewModel
    }
}
