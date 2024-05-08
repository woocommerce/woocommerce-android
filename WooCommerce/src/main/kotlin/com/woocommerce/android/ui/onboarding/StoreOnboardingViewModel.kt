package com.woocommerce.android.ui.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreOnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val onboardingRepository: StoreOnboardingRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val shouldShowOnboarding: ShouldShowOnboarding
) : ScopedViewModel(savedStateHandle), DefaultLifecycleObserver {
    companion object {
        const val NUMBER_ITEMS_IN_COLLAPSED_MODE = 3
    }

    private val _viewState = MutableLiveData(
        OnboardingState(
            show = false,
            title = R.string.store_onboarding_title,
            tasks = emptyList(),
        )
    )
    val viewState = _viewState

    init {
        launch {
            onboardingRepository.observeOnboardingTasks()
                .collectLatest { tasks ->
                    _viewState.value = OnboardingState(
                        show = shouldShowOnboarding.showForTasks(tasks),
                        title = R.string.store_onboarding_title,
                        tasks = tasks.map { it.toOnboardingTaskState() },
                    )
                }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        refreshOnboardingList()
    }

    fun viewAllClicked() {
        triggerEvent(NavigateToOnboardingFullScreen)
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onTaskClicked(task: OnboardingTaskUi) {
        when (task.taskUiResources) {
            AboutYourStoreTaskRes -> triggerEvent(NavigateToAboutYourStore)
            is AddProductTaskRes -> triggerEvent(NavigateToAddProduct)
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

    fun onPullToRefresh() {
        refreshOnboardingList()
    }

    private fun refreshOnboardingList() {
        if (!shouldShowOnboarding.isOnboardingMarkedAsCompleted()) {
            launch {
                onboardingRepository.fetchOnboardingTasks()
            }
        } else {
            _viewState.value = _viewState.value?.copy(show = false)
        }
    }

    data class OnboardingState(
        val show: Boolean,
        @StringRes val title: Int,
        val tasks: List<OnboardingTaskUi>,
    )
}
