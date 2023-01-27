package com.woocommerce.android.ui.login.storecreation.onboarding

import android.os.Parcelable
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
            tasks = emptyList()
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
        val name: String,
        val description: String,
        val status: OnboardingTaskStatus
    ) : Parcelable

    enum class OnboardingTaskStatus {
        TODO,
        COMPLETED
    }
}
