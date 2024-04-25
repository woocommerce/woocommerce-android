package com.woocommerce.android.ui.dashboard.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
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
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DashboardOnboardingViewModel.Factory::class)
class DashboardOnboardingViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val onboardingRepository: StoreOnboardingRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
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
                    DashboardWidgetAction(
                        titleResource = R.string.store_onboarding_menu_hide_store_setup,
                        action = ::onHideOnboardingClicked
                    )
                )
            ),
            onViewAllTapped = DashboardWidgetAction(
                titleResource = R.string.store_onboarding_task_view_all,
                action = ::viewAllClicked
            )
        )
    )
    val viewState = _viewState

    init {
        launch {
            parentViewModel.refreshTrigger
                .onStart { emit(RefreshEvent()) }
                .collectLatest {
                    _viewState.value = _viewState.value?.copy(isLoading = true)
                    onboardingRepository.fetchOnboardingTasks()
                }
        }
        launch {
            onboardingRepository.observeOnboardingTasks()
                .collectLatest { tasks ->
                    _viewState.value = _viewState.value?.copy(
                        tasks = tasks.map { it.toOnboardingTaskState() },
                        isLoading = false
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

    private fun onHideOnboardingClicked() {
        triggerEvent(NavigateToDashboardWidgetEditor)
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

    data class OnboardingDashBoardState(
        @StringRes val title: Int,
        val tasks: List<OnboardingTaskUi>,
        val menu: DashboardWidgetMenu,
        val onViewAllTapped: DashboardWidgetAction,
        val isLoading: Boolean = false
    )

    object NavigateToDashboardWidgetEditor : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardOnboardingViewModel
    }
}
