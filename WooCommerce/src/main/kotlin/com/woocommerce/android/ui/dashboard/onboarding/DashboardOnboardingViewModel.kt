package com.woocommerce.android.ui.dashboard.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_ADD_DOMAIN
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_LAUNCH_SITE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_LOCAL_NAME_STORE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PRODUCTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STORE_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_WOO_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
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
import com.woocommerce.android.ui.onboarding.ShouldShowOnboarding
import com.woocommerce.android.ui.onboarding.ShowNameYourStoreDialog
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.ABOUT_YOUR_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.ADD_FIRST_PRODUCT
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.CUSTOMIZE_DOMAIN
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.LAUNCH_YOUR_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.LOCAL_NAME_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.MOBILE_UNSUPPORTED
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.PAYMENTS
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.WC_PAYMENTS
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DashboardOnboardingViewModel.Factory::class)
class DashboardOnboardingViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val onboardingRepository: StoreOnboardingRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val shouldShowOnboarding: ShouldShowOnboarding
) : ScopedViewModel(savedStateHandle), DefaultLifecycleObserver {
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
            _viewState.value = _viewState.value?.copy(isLoading = true)
            onboardingRepository.observeOnboardingTasks()
                .collectLatest { tasks ->
                    _viewState.value = _viewState.value?.copy(
                        tasks = tasks.map { mapToOnboardingTaskState(it) },
                        isLoading = false
                    )
                }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        refreshOnboardingList()
    }

    private fun mapToOnboardingTaskState(task: OnboardingTask) =
        when (task.type) {
            ABOUT_YOUR_STORE -> OnboardingTaskUi(AboutYourStoreTaskRes, isCompleted = task.isComplete)
            LAUNCH_YOUR_STORE -> OnboardingTaskUi(LaunchStoreTaskRes, isCompleted = task.isComplete)
            CUSTOMIZE_DOMAIN -> OnboardingTaskUi(CustomizeDomainTaskRes, isCompleted = task.isComplete)
            WC_PAYMENTS -> OnboardingTaskUi(SetupWooPaymentsTaskRes, isCompleted = task.isComplete)
            PAYMENTS -> OnboardingTaskUi(SetupPaymentsTaskRes, isCompleted = task.isComplete)
            ADD_FIRST_PRODUCT -> OnboardingTaskUi(AddProductTaskRes, isCompleted = task.isComplete)
            LOCAL_NAME_STORE -> OnboardingTaskUi(NameYourStoreTaskRes, isCompleted = task.isComplete)
            MOBILE_UNSUPPORTED -> error("Unknown task type is not allowed in UI layer")
        }

    private fun viewAllClicked() {
        triggerEvent(NavigateToOnboardingFullScreen)
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun onShareFeedbackClicked() {
        triggerEvent(NavigateToSurvey)
    }

    private fun onHideOnboardingClicked() {
        // TODO open widget editor
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
            properties = mapOf(AnalyticsTracker.ONBOARDING_TASK_KEY to getTaskTrackingKey(task))
        )
    }

    private fun getTaskTrackingKey(task: OnboardingTaskUi) =
        when (task.taskUiResources) {
            AboutYourStoreTaskRes -> VALUE_STORE_DETAILS
            is AddProductTaskRes -> VALUE_PRODUCTS
            CustomizeDomainTaskRes -> VALUE_ADD_DOMAIN
            LaunchStoreTaskRes -> VALUE_LAUNCH_SITE
            SetupPaymentsTaskRes -> VALUE_PAYMENTS
            SetupWooPaymentsTaskRes -> VALUE_WOO_PAYMENTS
            NameYourStoreTaskRes -> VALUE_LOCAL_NAME_STORE
        }

    fun onPullToRefresh() {
        refreshOnboardingList()
    }

    private fun refreshOnboardingList() {
        if (!shouldShowOnboarding.isOnboardingMarkedAsCompleted()) {
            launch {
                onboardingRepository.fetchOnboardingTasks()
            }
        }
    }

    data class OnboardingDashBoardState(
        @StringRes val title: Int,
        val tasks: List<OnboardingTaskUi>,
        val menu: DashboardWidgetMenu,
        val onViewAllTapped: DashboardWidgetAction,
        val isLoading: Boolean = false
    )

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardOnboardingViewModel
    }
}
