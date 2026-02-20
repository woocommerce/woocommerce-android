package com.woocommerce.android.ui.pushnotifications.connection

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
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
import org.wordpress.android.fluxc.store.JetpackStore
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class WooPushNotificationsConnectionStepsViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val pushNotificationRepository: PushNotificationRepository,
    private val jetpackActivationRepository: JetpackActivationRepository,
    private val stringUtils: StringUtils,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val siteAddress = getSiteAddress()

    private val currentStep = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = Step(type = StepType.CheckPluginCompatibility),
        key = KEY_CURRENT_STEP
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
        if (currentStep.value.state == StepState.Idle) {
            startNextStep()
        }
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
                    StepType.ConnectStore -> connectStoreToJetpack()

                    StepType.CheckPluginCompatibility -> {
                        delay(1.seconds)
                        advanceToNextStep()
                    }

                    StepType.EnablePushNotifications -> registerPushNotifications()
                }
            }
    }

    private suspend fun connectStoreToJetpack() {
        jetpackActivationRepository.registerSite(
            site = selectedSite.get(),
            useApplicationPasswords = true
        ).fold(
            onSuccess = {
                markCurrentStepAsCompleted()
                advanceToNextStep()
            },
            onFailure = { error ->
                markCurrentStepAsFailed(resolveConnectionErrorMessage(error))
            }
        )
    }

    private fun markCurrentStepAsCompleted() {
        currentStep.update { it.copy(state = StepState.Success) }
    }

    private fun markCurrentStepAsFailed(@StringRes messageRes: Int) {
        currentStep.update { current ->
            current.copy(state = StepState.Error(messageRes))
        }
    }

    private fun resolveConnectionErrorMessage(error: Throwable): Int {
        return if ((error as? OnChangedException)
                ?.error
                ?.let { it as? JetpackStore.JetpackError }
                ?.errorCode == ERROR_CODE_FORBIDDEN
        ) {
            R.string.woo_push_notifications_connection_steps_error_connection_permission_message
        } else {
            R.string.woo_push_notifications_connection_steps_generic_error
        }
    }

    @Suppress("unused")
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
            onSuccess = { markCurrentStepAsCompleted() },
            onFailure = {
                currentStep.update {
                    it.copy(state = StepState.Error(R.string.woo_push_notifications_connection_steps_generic_error))
                }
            }
        )
    }

    private fun getSiteAddress(): String {
        val site = selectedSite.get()
        return stringUtils.getSiteDomainAndPath(site).ifBlank { site.name.orEmpty() }
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
        CheckPluginCompatibility,
        ConnectStore,
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

    companion object {
        private const val ERROR_CODE_FORBIDDEN = 403

        @VisibleForTesting
        internal const val KEY_CURRENT_STEP = "woo-push-connection-current-step"
    }
}
