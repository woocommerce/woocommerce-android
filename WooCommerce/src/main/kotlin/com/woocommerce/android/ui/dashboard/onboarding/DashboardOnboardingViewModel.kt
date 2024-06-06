package com.woocommerce.android.ui.dashboard.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

@HiltViewModel(assistedFactory = DashboardOnboardingViewModel.Factory::class)
class DashboardOnboardingViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val onboardingRepository: StoreOnboardingRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    val addProductNavigator: AddProductNavigator
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val MAX_NUMBER_OF_TASK_TO_DISPLAY_IN_CARD = 3
    }

    private val refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)

    private val initialState = OnboardingDashBoardState(
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
        onViewAllTapped = ::viewAllClicked
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = merge(parentViewModel.refreshTrigger, refreshTrigger)
        .onStart { emit(RefreshEvent()) }
        .transformLatest { refreshEvent ->
            emit(initialState.copy(isLoading = true, isError = false))

            val shouldFetch = refreshEvent.isForced || !onboardingRepository.hasCachedTasks
            if (shouldFetch) {
                onboardingRepository.fetchOnboardingTasks().onFailure {
                    emit(initialState.copy(isLoading = false, isError = true))
                    return@transformLatest
                }
            }

            emitAll(
                onboardingRepository.observeOnboardingTasks()
                    .map { tasks ->
                        initialState.copy(
                            isLoading = false,
                            tasks = tasks.map { it.toOnboardingTaskState() }
                        )
                    }
            )
        }.asLiveData()

    private fun viewAllClicked() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.ONBOARDING.trackingIdentifier)
        triggerEvent(NavigateToOnboardingFullScreen)
    }

    private fun onShareFeedbackClicked() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.ONBOARDING.trackingIdentifier)
        triggerEvent(NavigateToSurvey)
    }

    fun onTaskClicked(task: OnboardingTaskUi) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.ONBOARDING.trackingIdentifier)
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
        private val onViewAllTapped: () -> Unit
    ) {
        val cardButton = if (!isLoading) {
            DashboardWidgetAction(
                titleResource = R.string.store_onboarding_task_view_all_tasks,
                action = onViewAllTapped
            )
        } else {
            null
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardOnboardingViewModel
    }
}
