package com.woocommerce.android.ui.pushnotifications.connection

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class WooPushNotificationsConnectionStepsViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val pushNotificationRepository: PushNotificationRepository,
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

    fun onContactSupportClick() {
        triggerEvent(Event.NavigateToHelpScreen(HelpOrigin.WOO_PUSH_NOTIFICATIONS_SETUP))
    }

    private fun startNextStep() {
        currentStep.update { it.copy(state = StepState.Ongoing) }
    }

    private fun monitorCurrentStep() = launch {
        currentStep
            .collectLatest { step ->
                if (step.state != StepState.Ongoing) return@collectLatest

                when (step.type) {
                    StepType.ConnectStore -> { // TODO
                        delay(1.seconds)
                        advanceToNextStep()
                    }
                    StepType.CheckPluginCompatibility -> { // TODO
                        delay(1.seconds)
                        advanceToNextStep()
                    }
                    StepType.EnablePushNotifications -> registerPushNotifications()
                }
            }
    }

    private fun advanceToNextStep() {
        currentStep.update { current ->
            val nextType = StepType.entries.getOrNull(current.type.ordinal + 1)
            if (nextType != null) {
                Step(type = nextType, state = StepState.Ongoing)
            } else {
                current.copy(state = StepState.Success)
            }
        }
    }

    private suspend fun registerPushNotifications() {
        val token = appPrefsWrapper.getFCMToken()
        if (token.isEmpty()) {
            currentStep.update {
                it.copy(state = StepState.Error(R.string.woo_push_notifications_connection_steps_generic_error))
            }
            return
        }

        val site = selectedSite.get()
        pushNotificationRepository.registerPushTokenInWooCoreSystem(token, site).fold(
            onSuccess = { currentStep.update { it.copy(state = StepState.Success) } },
            onFailure = {
                currentStep.update {
                    it.copy(state = StepState.Error(R.string.woo_push_notifications_connection_steps_generic_error))
                }
            }
        )
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
        val isError = steps.any { it.state is StepState.Error }
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
        data class Error(val errorMessage: UiString) : StepState {
            constructor(message: String) : this(UiString.UiStringText(message))
            constructor(@StringRes messageRes: Int) : this(UiString.UiStringRes(messageRes))
        }
    }
}
