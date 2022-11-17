package com.woocommerce.android.ui.login.jetpack.main

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

private const val STEPS_SAVED_STATE_KEY = "steps"

@HiltViewModel
class JetpackActivationMainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jetpackActivationRepository: JetpackActivationRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationMainFragmentArgs by savedStateHandle.navArgs()
    private val site: Deferred<SiteModel>
        get() = async {
            requireNotNull(jetpackActivationRepository.getSiteByUrl(navArgs.siteUrl)) {
                "Site not cached"
            }
        }
    private val steps = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = if (navArgs.isJetpackInstalled) stepsForConnection() else stepsForInstallation(),
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

    private fun stepsForInstallation() = listOf(
        Step(type = StepType.Installation),
        Step(type = StepType.Activation),
        Step(type = StepType.Connection),
        Step(type = StepType.Done)
    )

    private fun stepsForConnection() = listOf(
        Step(type = StepType.Connection),
        Step(type = StepType.Done)
    )

    data class ViewState(
        val siteUrl: String,
        val isJetpackInstalled: Boolean,
        val steps: List<Step>
    )

    @Parcelize
    data class Step(
        val type: StepType,
        val state: StepState = StepState.Idle
    ) : Parcelable

    enum class StepType {
        Installation, Activation, Connection, Done
    }

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
