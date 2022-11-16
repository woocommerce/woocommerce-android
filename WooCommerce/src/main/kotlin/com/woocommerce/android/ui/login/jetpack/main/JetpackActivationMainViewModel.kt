package com.woocommerce.android.ui.login.jetpack.main

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class JetpackActivationMainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationMainFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(
            siteUrl = navArgs.siteUrl,
            isJetpackInstalled = navArgs.isJetpackInstalled,
            steps = emptyList()
        )
    )
    val viewState = _viewState.asLiveData()

    @Parcelize
    data class ViewState(
        val siteUrl: String,
        val isJetpackInstalled: Boolean,
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
