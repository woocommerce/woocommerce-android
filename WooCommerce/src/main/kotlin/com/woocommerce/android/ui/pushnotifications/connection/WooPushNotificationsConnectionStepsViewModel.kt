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
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.model.UiString
import com.woocommerce.android.notifications.push.CheckWooPluginPushNotificationsSupport
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val siteAddress = getSiteAddress()
    private val navArgs by savedStateHandle.navArgs<WooPushNotificationsConnectionStepsFragmentArgs>()

    private val visibleStepTypes = StepType.entries.filter {
        it != StepType.ConnectStore || !navArgs.isSiteConnectedToJetpack
    }

    private val titleRes = if (navArgs.isSiteConnectedToJetpack) {
        R.string.woo_push_notifications_connection_steps_title_setup
    } else {
        R.string.woo_push_notifications_connection_steps_title_connect
    }

    private val bodyRes = if (navArgs.isSiteConnectedToJetpack) {
        R.string.woo_push_notifications_connection_steps_body_setup
    } else {
        R.string.woo_push_notifications_connection_steps_body_connect
    }

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
        flowOf(visibleStepTypes)
    ) { currentStep, stepTypes ->
        ViewState(
            titleRes = titleRes,
            bodyRes = bodyRes,
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
            // Not using mark markCurrentStepAsFailed as we don't want to track this as failure.
            currentStep.update { current ->
                current.copy(
                    state = StepState.Error(
                        UiString.UiStringRes(R.string.woo_push_notifications_connection_steps_generic_error)
                    )
                )
            }
            val url = selectedSite.get().adminUrlOrDefault.slashJoin(WC_PLUGIN_UPDATE_PATH)
            triggerEvent(NavigateToPluginUpdatePage(url))
            return
        }

        when (val result = checkWCPluginSupport(forceRefresh = true)) {
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
                    error = Exception("Plugin update required."),
                    errorType = StepState.ErrorType.PLUGIN_UPDATE_REQUIRED
                )
            }

            CheckWooPluginPushNotificationsSupport.Result.Error -> {
                markCurrentStepAsFailed(
                    messageRes = R.string.woo_push_notifications_connection_steps_generic_error,
                    error = Exception("Plugin check failed.")
                )
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
                markCurrentStepAsFailed(resolveConnectionErrorMessage(error), error)
            }
        )
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

    private fun markCurrentStepAsCompleted() {
        trackFlowSuccess(currentStep.value.type)
        currentStep.update { it.copy(state = StepState.Success) }
    }

    private fun markCurrentStepAsFailed(
        @StringRes messageRes: Int,
        error: Throwable? = null,
        errorType: StepState.ErrorType = StepState.ErrorType.GENERIC_ERROR
    ) = markCurrentStepAsFailed(UiString.UiStringRes(messageRes), error, errorType)

    private fun markCurrentStepAsFailed(
        message: UiString,
        error: Throwable? = null,
        errorType: StepState.ErrorType = StepState.ErrorType.GENERIC_ERROR
    ) {
        trackFlowError(currentStep.value.type, error)
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
            val currentIndex = visibleStepTypes.indexOf(current.type)
            val nextType = visibleStepTypes.getOrNull(currentIndex + 1)
            if (nextType != null) {
                Step(type = nextType, state = StepState.Ongoing)
            } else {
                current.copy(state = StepState.Success)
            }
        }
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
        @StringRes val titleRes: Int,
        @StringRes val bodyRes: Int,
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
        data class Error(
            val errorMessage: UiString,
            val errorType: ErrorType = ErrorType.GENERIC_ERROR
        ) : StepState

        enum class ErrorType {
            GENERIC_ERROR,
            PLUGIN_UPDATE_REQUIRED
        }
    }

    data class NavigateToPluginUpdatePage(val url: String) : Event()

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
        internal const val WC_PLUGIN_UPDATE_PATH =
            "plugin-install.php?tab=plugin-information&plugin=woocommerce"

        @VisibleForTesting
        internal const val KEY_CURRENT_STEP = "woo-push-connection-current-step"

        private const val KEY_HAS_AUTO_OPENED_UPDATE = "woo-push-has-auto-opened-update"
    }
}
