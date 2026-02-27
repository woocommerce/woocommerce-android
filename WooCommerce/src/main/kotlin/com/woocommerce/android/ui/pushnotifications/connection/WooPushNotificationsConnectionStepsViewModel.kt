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
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
import com.woocommerce.android.viewmodel.navArgs
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val siteAddress = getSiteAddress()
    private val navArgs by savedStateHandle.navArgs<WooPushNotificationsConnectionStepsFragmentArgs>()

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
        trackFlowButtonTap(BUTTON_LABEL_GO_TO_MY_STORE)
        trackFlowClose()
        triggerEvent(Exit)
    }

    fun onCloseClick() {
        trackFlowClose()
        triggerEvent(Exit)
    }

    fun onRetryClick() {
        trackFlowButtonTap(BUTTON_LABEL_TRY_AGAIN)
        startNextStep()
    }

    fun onContactSupportClick() {
        trackFlowButtonTap(BUTTON_LABEL_SUPPORT)
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
                        markCurrentStepAsCompleted()
                        advanceToNextStep()
                    }

                    StepType.EnablePushNotifications -> registerPushNotifications()
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
                markCurrentStepAsFailed(resolveConnectionErrorMessage(error), error)
            }
        )
    }

    private fun markCurrentStepAsCompleted() {
        trackFlowSuccess(currentStep.value.type)
        currentStep.update { it.copy(state = StepState.Success) }
    }

    private fun markCurrentStepAsFailed(@StringRes messageRes: Int, error: Throwable? = null) {
        trackFlowError(currentStep.value.type, error)
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
            markCurrentStepAsFailed(
                R.string.woo_push_notifications_connection_steps_generic_error,
                IllegalStateException(EMPTY_FCM_TOKEN_ERROR_DESCRIPTION)
            )
            return
        }

        val site = selectedSite.get()
        pushNotificationRepository.registerPushTokenInWooCoreSystem(token, site).fold(
            onSuccess = { markCurrentStepAsCompleted() },
            onFailure = { error ->
                markCurrentStepAsFailed(R.string.woo_push_notifications_connection_steps_generic_error, error)
            }
        )
    }

    private fun trackFlowSuccess(stepType: StepType) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_SUCCESS,
            mapOf(AnalyticsTracker.KEY_STEP to stepType.trackingValue)
        )
    }

    private fun trackFlowError(stepType: StepType, error: Throwable?) {
        val properties = mutableMapOf<String, String>()
        properties[AnalyticsTracker.KEY_STEP] = stepType.trackingValue
        error.errorDescription()?.let { properties[AnalyticsTracker.KEY_ERROR_DESC] = it }
        error.errorCode()?.let { properties[AnalyticsTracker.KEY_ERROR_CODE] = it }
        error.errorType()?.let { properties[AnalyticsTracker.KEY_ERROR_TYPE] = it }
        analyticsTrackerWrapper.track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_ERROR, properties)
    }

    private fun trackFlowButtonTap(buttonLabel: String) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_BUTTON_TAP,
            mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to buttonLabel)
        )
    }

    private fun trackFlowClose() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_CLOSE)
    }

    private fun Throwable?.errorDescription(): String? = when (this) {
        null -> null
        is OnChangedException -> (error as? JetpackStore.JetpackError)?.message ?: message
        is WooException -> error.message
        else -> message
    }

    private fun Throwable?.errorCode(): String? = when (this) {
        null -> null
        is OnChangedException -> (error as? JetpackStore.JetpackError)?.errorCode?.toString()
        is WooException -> error.apiErrorCode
        else -> null
    }

    private fun Throwable?.errorType(): String? = when (this) {
        null -> null
        is OnChangedException -> {
            (error as? JetpackStore.JetpackError)?.let { it::class.simpleName } ?: javaClass.simpleName
        }
        is WooException -> error.type.name
        else -> javaClass.simpleName
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

    private val StepType.trackingValue: String
        get() = when (this) {
            StepType.CheckPluginCompatibility -> STEP_PLUGIN_COMPATIBILITY
            StepType.ConnectStore -> STEP_CONNECT_WPCOM
            StepType.EnablePushNotifications -> STEP_ENABLE_PUSH_NOTIFICATIONS
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
        private const val EMPTY_FCM_TOKEN_ERROR_DESCRIPTION = "FCM token is empty."

        private const val STEP_CONNECT_WPCOM = "connect_wpcom"
        private const val STEP_PLUGIN_COMPATIBILITY = "plugin_compatibility"
        private const val STEP_ENABLE_PUSH_NOTIFICATIONS = "enable_push_notifications"

        private const val BUTTON_LABEL_TRY_AGAIN = "try_again"
        private const val BUTTON_LABEL_GO_TO_MY_STORE = "go_to_my_store"
        private const val BUTTON_LABEL_SUPPORT = "support"

        @VisibleForTesting
        internal const val KEY_CURRENT_STEP = "woo-push-connection-current-step"
    }
}
