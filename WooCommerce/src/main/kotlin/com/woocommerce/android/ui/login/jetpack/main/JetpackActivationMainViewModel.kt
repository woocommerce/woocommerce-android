package com.woocommerce.android.ui.login.jetpack.main

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val STEPS_SAVED_STATE_KEY = "steps"

@HiltViewModel
class JetpackActivationMainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationMainFragmentArgs by savedStateHandle.navArgs()

    private val steps = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        // Just for demo
        initialValue = listOf(
            Step(
                title = R.string.login_jetpack_steps_installing,
                state = StepState.Success
            ),
            Step(
                title = R.string.login_jetpack_steps_activating,
                state = StepState.Ongoing
            ),
            Step(
                title = R.string.login_jetpack_steps_activating,
                state = StepState.Error,
                additionalInfo = UiStringRes(
                    R.string.login_jetpack_installation_error_code_template,
                    listOf(UiStringText("403"))
                )
            ),
            Step(
                title = R.string.login_jetpack_steps_authorizing,
                state = StepState.Idle,
                additionalInfo = UiStringRes(R.string.login_jetpack_steps_authorizing_hint)
            ),
            Step(
                title = R.string.login_jetpack_steps_done,
                state = StepState.Idle
            )
        ),
        key = STEPS_SAVED_STATE_KEY
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
