package com.woocommerce.android.ui.login.storecreation.onboarding

import androidx.annotation.DrawableRes
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
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PRODUCTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STORE_DETAILS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.ABOUT_YOUR_STORE
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.ADD_FIRST_PRODUCT
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.CUSTOMIZE_DOMAIN
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.LAUNCH_YOUR_STORE
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.MOBILE_UNSUPPORTED
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.PAYMENTS
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.WC_PAYMENTS
import com.woocommerce.android.util.FeatureFlag
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle), DefaultLifecycleObserver {
    companion object {
        const val NUMBER_ITEMS_IN_COLLAPSED_MODE = 3
    }

    private val _viewState = MutableLiveData<OnboardingState>()
    val viewState = _viewState

    init {
        launch {
            onboardingRepository.onboardingTasksCacheFlow
                .collectLatest { tasks ->
                    _viewState.value = OnboardingState(
                        show = tasks.any { !it.isComplete } && FeatureFlag.STORE_CREATION_ONBOARDING.isEnabled(),
                        title = R.string.store_onboarding_title,
                        tasks = tasks.map { mapToOnboardingTaskState(it) },
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
            ADD_FIRST_PRODUCT -> OnboardingTaskUi(AddProductTaskRes, isCompleted = task.isComplete)
            LAUNCH_YOUR_STORE -> OnboardingTaskUi(LaunchStoreTaskRes, isCompleted = task.isComplete)
            CUSTOMIZE_DOMAIN -> OnboardingTaskUi(CustomizeDomainTaskRes, isCompleted = task.isComplete)
            WC_PAYMENTS,
            PAYMENTS -> OnboardingTaskUi(SetupPaymentsTaskRes, isCompleted = task.isComplete)
            MOBILE_UNSUPPORTED -> error("Unknown task type is not allowed in UI layer")
        }

    fun viewAllClicked() {
        triggerEvent(NavigateToOnboardingFullScreen)
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onShareFeedbackClicked() {
        triggerEvent(NavigateToSurvey)
    }

    fun onTaskClicked(task: OnboardingTaskUi) {
        when (task.taskUiResources) {
            AboutYourStoreTaskRes -> triggerEvent(NavigateToAboutYourStore)
            AddProductTaskRes -> triggerEvent(NavigateToAddProduct)
            CustomizeDomainTaskRes -> triggerEvent(NavigateToDomains)
            LaunchStoreTaskRes -> triggerEvent(NavigateToLaunchStore)
            SetupPaymentsTaskRes -> triggerEvent(NavigateToSetupPayments)
        }
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.STORE_ONBOARDING_TASK_TAPPED,
            properties = mapOf(AnalyticsTracker.ONBOARDING_TASK_KEY to getTaskTrackingKey(task))
        )
    }

    private fun getTaskTrackingKey(task: OnboardingTaskUi) =
        when (task.taskUiResources) {
            AboutYourStoreTaskRes -> VALUE_STORE_DETAILS
            AddProductTaskRes -> VALUE_PRODUCTS
            CustomizeDomainTaskRes -> VALUE_ADD_DOMAIN
            LaunchStoreTaskRes -> VALUE_LAUNCH_SITE
            SetupPaymentsTaskRes -> VALUE_PAYMENTS
        }

    fun onPullToRefresh() {
        refreshOnboardingList()
    }

    private fun refreshOnboardingList() {
        if (!onboardingRepository.isOnboardingCompleted()) {
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

    data class OnboardingTaskUi(
        val taskUiResources: OnboardingTaskUiResources,
        val isCompleted: Boolean,
    )

    sealed class OnboardingTaskUiResources(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        @StringRes val description: Int
    )

    object AboutYourStoreTaskRes : OnboardingTaskUiResources(
        icon = R.drawable.ic_onboarding_about_your_store,
        title = R.string.store_onboarding_task_about_your_store_title,
        description = R.string.store_onboarding_task_about_your_store_description
    )

    object AddProductTaskRes : OnboardingTaskUiResources(
        icon = R.drawable.ic_onboarding_add_product,
        title = R.string.store_onboarding_task_add_product_title,
        description = R.string.store_onboarding_task_add_product_description
    )

    object LaunchStoreTaskRes : OnboardingTaskUiResources(
        icon = R.drawable.ic_onboarding_launch_store,
        title = R.string.store_onboarding_task_launch_store_title,
        description = R.string.store_onboarding_task_launch_store_description
    )

    object CustomizeDomainTaskRes : OnboardingTaskUiResources(
        icon = R.drawable.ic_onboarding_customize_domain,
        title = R.string.store_onboarding_task_change_domain_title,
        description = R.string.store_onboarding_task_change_domain_description
    )

    object SetupPaymentsTaskRes : OnboardingTaskUiResources(
        icon = R.drawable.ic_onboarding_payments_setup,
        title = R.string.store_onboarding_task_payments_setup_title,
        description = R.string.store_onboarding_task_payments_setup_description
    )

    object NavigateToOnboardingFullScreen : MultiLiveEvent.Event()
    object NavigateToSurvey : MultiLiveEvent.Event()
    object NavigateToLaunchStore : MultiLiveEvent.Event()
    object NavigateToDomains : MultiLiveEvent.Event()
    object NavigateToSetupPayments : MultiLiveEvent.Event()
    object NavigateToAboutYourStore : MultiLiveEvent.Event()
    object NavigateToAddProduct : MultiLiveEvent.Event()
}
