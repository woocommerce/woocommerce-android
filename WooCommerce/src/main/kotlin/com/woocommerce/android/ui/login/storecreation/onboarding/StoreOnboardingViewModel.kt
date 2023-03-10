package com.woocommerce.android.ui.login.storecreation.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.ABOUT_YOUR_STORE
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.ADD_FIRST_PRODUCT
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.CUSTOMIZE_DOMAIN
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.LAUNCH_YOUR_STORE
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.MOBILE_UNSUPPORTED
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.WC_PAYMENTS
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ONBOARDING
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreOnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val onboardingRepository: StoreOnboardingRepository
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val NUMBER_ITEMS_IN_COLLAPSED_MODE = 3
    }

    private val _viewState = savedState.getStateFlow(
        this,
        OnboardingState(
            show = false,
            title = R.string.store_onboarding_title,
            tasks = emptyList(),
        )
    )
    val viewState = _viewState.asLiveData()

    init {
        launch {
            val result = onboardingRepository.fetchOnboardingTasks()
            if (result.isNotEmpty()) {
                _viewState.update { currentUiState ->
                    currentUiState.copy(
                        show = result.any { !it.isComplete } && FeatureFlag.STORE_CREATION_ONBOARDING.isEnabled(),
                        title = R.string.store_onboarding_title,
                        tasks = result.map { mapToOnboardingTaskState(it) }
                    )
                }
            }
        }
    }

    private fun mapToOnboardingTaskState(task: OnboardingTask) =
        when (task.type) {
            ABOUT_YOUR_STORE -> OnboardingTaskUi(AboutYourStoreTaskRes, isCompleted = task.isComplete)
            ADD_FIRST_PRODUCT -> OnboardingTaskUi(AddProductTaskRes, isCompleted = task.isComplete)
            LAUNCH_YOUR_STORE -> OnboardingTaskUi(LaunchStoreTaskRes, isCompleted = task.isComplete)
            CUSTOMIZE_DOMAIN -> OnboardingTaskUi(CustomizeDomainTaskRes, isCompleted = task.isComplete)
            WC_PAYMENTS -> OnboardingTaskUi(SetupPaymentsTaskRes, isCompleted = task.isComplete)
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
            AboutYourStoreTaskRes -> WooLog.d(ONBOARDING, "TODO")
            AddProductTaskRes -> WooLog.d(ONBOARDING, "TODO")
            CustomizeDomainTaskRes -> WooLog.d(ONBOARDING, "TODO")
            LaunchStoreTaskRes -> triggerEvent(NavigateToLaunchStore)
            SetupPaymentsTaskRes -> WooLog.d(ONBOARDING, "TODO")
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
}
