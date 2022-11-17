package com.woocommerce.android.ui.login.jetpack.main

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class JetpackActivationMainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationMainFragmentArgs by savedStateHandle.navArgs()

    private val steps = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = emptyList<Step>()
    )
    val viewState = steps.map {
        ViewState(
            siteUrl = navArgs.siteUrl,
            isJetpackInstalled = navArgs.isJetpackInstalled,
            steps = it
        )
    }.asLiveData()

    fun onCloseClick() {
        triggerEvent(Exit)
    }

    data class ViewState(
        val siteUrl: String,
        val isJetpackInstalled: Boolean,
        val steps: List<Step>
    )

    @Parcelize
    data class Step(
        @StringRes val title: Int,
        val state: StepState,
        val additionalInfo: UiString? = null
    ) : Parcelable

    enum class StepState {
        Idle, Ongoing, Success, Error
    }
}
