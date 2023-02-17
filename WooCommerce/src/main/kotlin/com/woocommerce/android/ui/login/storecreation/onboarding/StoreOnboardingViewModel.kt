package com.woocommerce.android.ui.login.storecreation.onboarding

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class StoreOnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val onboardingRepository: StoreOnboardingRepository
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = savedState.getStateFlow(
        this,
        OnboardingState(
            show = false,
            title = R.string.store_onboarding_title,
            tasks = emptyList()
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
                        tasks = result.map { it.toOnboardingTaskUi() }
                    )
                }
            }
        }
    }

    private fun OnboardingTask.toOnboardingTaskUi() =
        OnboardingTaskUi(
            icon = getIconResource(),
            title = getTitleStringResource(),
            description = getDescriptionStringResource(),
            isCompleted = isComplete,
            isVisible = isVisible
        )

    @DrawableRes
    private fun OnboardingTask.getIconResource() =
        when (this.type) {
            OnboardingTaskType.ABOUT_YOUR_STORE -> R.drawable.ic_onboarding_about_your_store
            OnboardingTaskType.ADD_FIRST_PRODUCT -> R.drawable.ic_onboarding_add_product
            OnboardingTaskType.LAUNCH_YOUR_STORE -> R.drawable.ic_onboarding_launch_store
            OnboardingTaskType.CUSTOMIZE_DOMAIN -> R.drawable.ic_onboarding_customize_domain
            OnboardingTaskType.WC_PAYMENTS -> R.drawable.ic_onboarding_payments_setup
            OnboardingTaskType.UNKNOWN -> error("UNKNOWN task type is not allowed in UI layer")
        }

    @StringRes
    private fun OnboardingTask.getTitleStringResource() =
        when (this.type) {
            OnboardingTaskType.ABOUT_YOUR_STORE -> R.string.store_onboarding_task_about_your_store_title
            OnboardingTaskType.ADD_FIRST_PRODUCT -> R.string.store_onboarding_task_add_product_title
            OnboardingTaskType.LAUNCH_YOUR_STORE -> R.string.store_onboarding_task_launch_store_title
            OnboardingTaskType.CUSTOMIZE_DOMAIN -> R.string.store_onboarding_task_change_domain_title
            OnboardingTaskType.WC_PAYMENTS -> R.string.store_onboarding_task_payments_setup_title
            OnboardingTaskType.UNKNOWN -> error("UNKNOWN task type is not allowed in UI layer")
        }

    @StringRes
    private fun OnboardingTask.getDescriptionStringResource() =
        when (this.type) {
            OnboardingTaskType.ABOUT_YOUR_STORE -> R.string.store_onboarding_task_about_your_store_description
            OnboardingTaskType.ADD_FIRST_PRODUCT -> R.string.store_onboarding_task_add_product_description
            OnboardingTaskType.LAUNCH_YOUR_STORE -> R.string.store_onboarding_task_launch_store_description
            OnboardingTaskType.CUSTOMIZE_DOMAIN -> R.string.store_onboarding_task_change_domain_description
            OnboardingTaskType.WC_PAYMENTS -> R.string.store_onboarding_task_payments_setup_description
            OnboardingTaskType.UNKNOWN -> error("UNKNOWN task type is not allowed in UI layer")
        }

    @Parcelize
    data class OnboardingState(
        val show: Boolean,
        @StringRes val title: Int,
        val tasks: List<OnboardingTaskUi>
    ) : Parcelable

    @Parcelize
    data class OnboardingTaskUi(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        @StringRes val description: Int,
        val isCompleted: Boolean,
        val isVisible: Boolean,
    ) : Parcelable
}
