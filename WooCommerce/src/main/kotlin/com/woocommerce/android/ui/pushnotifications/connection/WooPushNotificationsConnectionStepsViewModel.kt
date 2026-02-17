package com.woocommerce.android.ui.pushnotifications.connection

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsConnectionStepsViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val siteAddress = getSiteAddress()

    private val currentStep = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = Step(StepType.ConnectStore),
    )

    val viewState = combine(
        currentStep,
        flowOf(StepType.entries.toList())
    ) { currentStep, stepTypes ->
        ViewState(
            siteAddress = siteAddress,
            steps = stepTypes.map { stepType ->
                Step(
                    type = stepType,
                    state = when {
                        currentStep.type == stepType -> currentStep.state
                        currentStep.type > stepType -> StepState.Success
                        else -> StepState.Idle
                    }
                )
            }
        )
    }.asLiveData()

    init {
        monitorCurrentStep()
        startNextStep()
    }

    fun onGoToStoreClick() {
        // TODO
        onCloseClick()
    }

    fun onCloseClick() {
        triggerEvent(Exit)
    }

    fun onRetryClick() {
        startNextStep()
    }

    private fun startNextStep() {
        currentStep.update { it.copy(state = StepState.Ongoing) }
    }

    private fun monitorCurrentStep() = launch {
        currentStep
            .collectLatest { step ->
                if (step.state != StepState.Ongoing) return@collectLatest

                @Suppress("NoOp")
                when (step.type) {
                    StepType.ConnectStore -> Unit
                    StepType.CheckPluginCompatibility -> Unit
                    StepType.EnablePushNotifications -> Unit
                }
            }
    }

    private fun getSiteAddress(): String {
        val site = selectedSite.get()
        return StringUtils.getSiteDomainAndPath(site).ifBlank { site.name.orEmpty() }
    }


    data class ViewState(
        val siteAddress: String,
        val steps: List<Step>
    ) {
        val isDone = steps.all { it.state == StepState.Success }
    }

    @Parcelize
    data class Step(
        val type: StepType,
        val state: StepState = StepState.Idle
    ) : Parcelable

    enum class StepType {
        ConnectStore,
        CheckPluginCompatibility,
        EnablePushNotifications
    }

    sealed interface StepState : Parcelable {
        @Parcelize
        data object Idle : StepState

        @Parcelize
        data object Ongoing : StepState

        @Parcelize
        data object Success : StepState

        @Parcelize
        data class Error(val errorMessage: UiString) : StepState
    }
}
