package com.woocommerce.android.ui.login.storecreation.onboarding

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class StoreOnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = savedState.getStateFlow(
        this,
        OnboardingState(
            show = FeatureFlag.STORE_CREATION_ONBOARDING.isEnabled(),
            title = R.string.store_onboarding_title,
            tasks = listOf(
                OnboardingTask(
                    icon = R.drawable.ic_product_onboarding_list,
                    title = R.string.store_onboarding_task_add_product_title,
                    description = R.string.store_onboarding_task_add_product_description,
                    status = OnboardingTaskStatus.UNDONE
                ),
                OnboardingTask(
                    icon = R.drawable.ic_store_onboarding_list,
                    title = R.string.store_onboarding_task_launch_store_title,
                    description = R.string.store_onboarding_task_launch_store_description,
                    status = OnboardingTaskStatus.UNDONE
                ),
                OnboardingTask(
                    icon = R.drawable.ic_globe_onboarding_list,
                    title = R.string.store_onboarding_task_change_domain_title,
                    description = R.string.store_onboarding_task_change_domain_description,
                    status = OnboardingTaskStatus.UNDONE
                )
            )
        )
    )
    val viewState = _viewState.asLiveData()

    @Parcelize
    data class OnboardingState(
        val show: Boolean,
        @StringRes val title: Int,
        val tasks: List<OnboardingTask>
    ) : Parcelable

    @Parcelize
    data class OnboardingTask(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        @StringRes val description: Int,
        val status: OnboardingTaskStatus
    ) : Parcelable

    enum class OnboardingTaskStatus {
        UNDONE,
        COMPLETED
    }
}
