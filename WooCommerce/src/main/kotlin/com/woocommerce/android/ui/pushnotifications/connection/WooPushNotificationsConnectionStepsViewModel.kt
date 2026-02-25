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
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.model.UiString
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.notifications.push.CheckWooPluginPushNotificationsSupport
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsConnectionStepsViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val pushNotificationRepository: PushNotificationRepository,
    private val jetpackActivationRepository: JetpackActivationRepository,
    private val checkWCPluginSupport: CheckWooPluginPushNotificationsSupport,
    private val stringUtils: StringUtils,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val siteAddress = getSiteAddress()
    private val navArgs by savedStateHandle.navArgs<WooPushNotificationsConnectionStepsFragmentArgs>()

    private var hasAutoOpenedUpdate: Boolean
        get() = savedState.get<Boolean>(KEY_HAS_AUTO_OPENED_UPDATE) ?: false
        set(value) {
            savedState[KEY_HAS_AUTO_OPENED_UPDATE] = value
        }

    private val currentStep = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = Step(type = StepType.CheckPluginCompatibility),
        key = KEY_CURRENT_STEP
    )

    val viewState = combine(
        currentStep,
        flowOf(StepType.entries.toList()),
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

    fun onUpdatePluginClick() {
        val url = selectedSite.get().adminUrlOrDefault.slashJoin(WC_PLUGIN_UPDATE_PATH)
        triggerEvent(NavigateToPluginUpdatePage(url))
    }

    fun onPluginUpdateWebViewDismissed() {
        onRetryClick()
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

                    StepType.CheckPluginCompatibility -> checkPluginCompatibility()

                    StepType.EnablePushNotifications -> registerPushNotifications()
                }
            }
    }

    private suspend fun checkPluginCompatibility() {
        if (navArgs.shouldAutoOpenUpdatePlugin && !hasAutoOpenedUpdate) {
            hasAutoOpenedUpdate = true
            markCurrentStepAsFailed(R.string.woo_push_notifications_connection_steps_generic_error)
            val url = selectedSite.get().adminUrlOrDefault.slashJoin(WC_PLUGIN_UPDATE_PATH)
            triggerEvent(NavigateToPluginUpdatePage(url))
            return
        }

        when (val result = checkWCPluginSupport()) {
            CheckWooPluginPushNotificationsSupport.Result.Compatible -> {
                markCurrentStepAsCompleted()
                advanceToNextStep()
            }

            is CheckWooPluginPushNotificationsSupport.Result.UpdateRequired -> {
                markCurrentStepAsFailed(
                    message = UiString.UiStringRes(
                        R.string.woo_push_notifications_connection_steps_error_plugin_update_required,
                        listOf(UiString.UiStringText(result.currentVersion))
                    ),
                    errorType = StepState.ErrorType.PLUGIN_UPDATE_REQUIRED
                )
            }

            CheckWooPluginPushNotificationsSupport.Result.Error -> {
                markCurrentStepAsFailed(R.string.woo_push_notifications_connection_steps_generic_error)
            }
        }
    }

    private suspend fun connectStoreToJetpack() {
        if (navArgs.isSiteConnectedToJetpack) {
            markCurrentStepAsCompleted()
            advanceToNextStep()
            return
        }

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

    private suspend fun registerPushNotifications() {
        val token = appPrefsWrapper.getFCMToken()
        if (token.isEmpty()) {
            markCurrentStepAsFailed(R.string.woo_push_notifications_connection_steps_generic_error)
            return
        }

        val site = selectedSite.get()
        pushNotificationRepository.registerPushTokenInWooCoreSystem(token, site).fold(
            onSuccess = { markCurrentStepAsCompleted() },
            onFailure = { markCurrentStepAsFailed(R.string.woo_push_notifications_connection_steps_generic_error) }
        )
    }

    private fun markCurrentStepAsCompleted() {
        currentStep.update { it.copy(state = StepState.Success) }
    }

    private fun markCurrentStepAsFailed(
        @StringRes messageRes: Int,
        errorType: StepState.ErrorType = StepState.ErrorType.GENERIC_ERROR
    ) {
        currentStep.update { current ->
            current.copy(state = StepState.Error(messageRes, errorType))
        }
    }

    private fun markCurrentStepAsFailed(
        message: UiString,
        errorType: StepState.ErrorType = StepState.ErrorType.GENERIC_ERROR
    ) {
        currentStep.update { current ->
            current.copy(state = StepState.Error(message, errorType))
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
        val isPluginUpdateRequired = steps.first { it.type == StepType.CheckPluginCompatibility }
            .let {
                it.state is StepState.Error && it.state.errorType == StepState.ErrorType.PLUGIN_UPDATE_REQUIRED
            }
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
        data class Error(
            val errorMessage: UiString,
            val errorType: ErrorType = ErrorType.GENERIC_ERROR
        ) : StepState {
            constructor(
                message: String,
                errorType: ErrorType = ErrorType.GENERIC_ERROR
            ) : this(UiString.UiStringText(message), errorType)

            constructor(
                @StringRes messageRes: Int,
                errorType: ErrorType = ErrorType.GENERIC_ERROR
            ) : this(UiString.UiStringRes(messageRes), errorType)
        }

        enum class ErrorType {
            GENERIC_ERROR,
            PLUGIN_UPDATE_REQUIRED
        }
    }

    data class NavigateToPluginUpdatePage(val url: String) : Event()

    companion object {
        private const val ERROR_CODE_FORBIDDEN = 403

        @VisibleForTesting
        internal const val WC_PLUGIN_UPDATE_PATH =
            "plugin-install.php?tab=plugin-information&plugin=woocommerce"

        @VisibleForTesting
        internal const val KEY_CURRENT_STEP = "woo-push-connection-current-step"

        private const val KEY_HAS_AUTO_OPENED_UPDATE = "woo-push-has-auto-opened-update"
    }
}
