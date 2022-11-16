package com.woocommerce.android.ui.login.jetpack.main

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class JetpackActivationMainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    @Parcelize
    data class ViewState(
        val steps: List<Step>
    ) : Parcelable

    @Parcelize
    data class Step(
        @StringRes val title: Int,
        val state: StepState,
        @StringRes val additionalInfo: Int? = null
    ) : Parcelable

    sealed interface StepState : Parcelable {
        @Parcelize
        object Idle : StepState

        @Parcelize
        object Ongoing : StepState

        @Parcelize
        object Success : StepState

        @Parcelize
        data class Error(val code: Int) : StepState
    }
}
