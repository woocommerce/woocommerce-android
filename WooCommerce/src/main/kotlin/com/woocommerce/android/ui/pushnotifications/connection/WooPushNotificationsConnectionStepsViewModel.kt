package com.woocommerce.android.ui.pushnotifications.connection

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.UiString
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.login.jetpack.JetpackConnectionUrlResolver
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewViewModel
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsConnectionStepsViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val jetpackActivationRepository: JetpackActivationRepository,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val siteAddress = getSiteAddress()

    private val currentStep = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = Step(type = StepType.ConnectStore),
        key = KEY_CURRENT_STEP
    )

    private val connectStoreStage = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ConnectStoreStage.FetchStatus,
        key = KEY_CONNECT_STORE_STAGE
    )

    private val pendingConnectionStatus = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = JetpackConnectionStatus.AccountNotConnected::class.java,
        key = KEY_PENDING_CONNECTION_STATUS
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

    fun onJetpackConnectionResult(result: JetpackActivationWebViewViewModel.ConnectionResult) {
        when (result) {
            JetpackActivationWebViewViewModel.ConnectionResult.Success -> {
                connectStoreStage.value = ConnectStoreStage.ConfirmConnection
            }

            JetpackActivationWebViewViewModel.ConnectionResult.Cancel -> {
                showConnectStoreStepError(R.string.woo_push_notifications_connection_steps_generic_error_message)
            }

            is JetpackActivationWebViewViewModel.ConnectionResult.Failure -> {
                showConnectStoreStepError(resolveConnectionErrorMessage(result.errorCode))
            }
        }
    }

    private fun startNextStep() {
        currentStep.update { it.copy(state = StepState.Ongoing) }
    }

    private fun monitorCurrentStep() = launch {
        combine(currentStep, connectStoreStage) { step, stage -> step to stage }
            .collectLatest { (step, stage) ->
                if (step.state != StepState.Ongoing) return@collectLatest

                when (step.type) {
                    StepType.ConnectStore -> {
                        when (stage) {
                            ConnectStoreStage.FetchStatus -> startFetchStatusStage()
                            ConnectStoreStage.WebViewConnection -> startWebViewConnectionStage()
                            ConnectStoreStage.ConnectAccount -> startConnectAccountStage()
                            ConnectStoreStage.ConfirmConnection -> startConfirmConnectionStage()
                        }
                    }

                    StepType.CheckPluginCompatibility -> Unit
                    StepType.EnablePushNotifications -> Unit
                }
            }
    }

    private suspend fun startFetchStatusStage() {
        val site = selectedSite.get()

        fetchJetpackStatus(site = site, useApplicationPasswords = true).fold(
            onSuccess = { response ->
                when (response) {
                    FetchJetpackStatus.JetpackStatusFetchResponse.ConnectionForbidden -> {
                        showConnectStoreStepError(
                            R.string.woo_push_notifications_connection_steps_error_connection_permission_message
                        )
                    }

                    is FetchJetpackStatus.JetpackStatusFetchResponse.Success -> {
                        when (val status = response.status.jetpackConnectionStatus) {
                            is JetpackConnectionStatus.AccountConnected -> showConnectStoreStepComplete()
                            is JetpackConnectionStatus.AccountNotConnected -> {
                                if (!status.supportsConnectionApi) {
                                    connectStoreStage.value = ConnectStoreStage.WebViewConnection
                                    return@fold
                                }

                                pendingConnectionStatus.value = status
                                connectStoreStage.value = ConnectStoreStage.ConnectAccount
                            }
                        }
                    }
                }
            },
            onFailure = {
                showConnectStoreStepError(R.string.woo_push_notifications_connection_steps_generic_error_message)
            }
        )
    }

    private suspend fun startWebViewConnectionStage() {
        val site = selectedSite.get()
        val siteUrl = requireNotNull(site.url?.takeIf { it.isNotBlank() }) { "Site URL missing" }

        jetpackActivationRepository.fetchJetpackConnectionUrl(
            site = site,
            useApplicationPasswords = true
        ).fold(
            onSuccess = { connectionUrl ->
                val connectionWebViewUrl = JetpackConnectionUrlResolver.resolveConnectionWebViewUrl(
                    connectionUrl = connectionUrl,
                    siteUrl = siteUrl
                )
                triggerEvent(ShowJetpackConnectionWebView(connectionWebViewUrl))
            },
            onFailure = { error ->
                showConnectStoreStepError(resolveConnectionErrorMessage(error))
            }
        )
    }

    private suspend fun startConnectAccountStage() {
        val status = pendingConnectionStatus.value
        if (status == null) {
            connectStoreStage.value = ConnectStoreStage.FetchStatus
            return
        }

        jetpackActivationRepository.connectJetpackAccount(
            site = selectedSite.get(),
            jetpackConnectionStatus = status,
            useApplicationPasswords = true
        ).fold(
            onSuccess = {
                connectStoreStage.value = ConnectStoreStage.ConfirmConnection
            },
            onFailure = { error ->
                showConnectStoreStepError(resolveConnectionErrorMessage(error))
            }
        )
    }

    private suspend fun startConfirmConnectionStage() {
        val siteUrl = requireNotNull(selectedSite.get().url?.takeIf { it.isNotBlank() }) { "Site URL missing" }

        jetpackActivationRepository.fetchJetpackSite(siteUrl).fold(
            onSuccess = {
                showConnectStoreStepComplete()
            },
            onFailure = {
                showConnectStoreStepError(R.string.woo_push_notifications_connection_steps_generic_error_message)
            }
        )
    }

    private fun showConnectStoreStepComplete() {
        pendingConnectionStatus.value = null
        currentStep.update { current ->
            current.copy(state = StepState.Success)
        }
    }

    private fun showConnectStoreStepError(@StringRes messageRes: Int) {
        currentStep.update { current ->
            current.copy(state = StepState.Error(messageRes))
        }
    }

    private fun resolveConnectionErrorMessage(error: Throwable): Int {
        val errorCode = (error as? OnChangedException)
            ?.error
            ?.let { it as? JetpackStore.JetpackError }
            ?.errorCode
        return resolveConnectionErrorMessage(errorCode)
    }

    private fun resolveConnectionErrorMessage(errorCode: Int?): Int {
        return if (errorCode == ERROR_CODE_FORBIDDEN) {
            R.string.woo_push_notifications_connection_steps_error_connection_permission_message
        } else {
            R.string.woo_push_notifications_connection_steps_generic_error_message
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

    private fun getSiteAddress(): String {
        val site = selectedSite.get()
        return UrlUtils.removeScheme(site.url.orEmpty()).ifBlank {
            StringUtils.getSiteDomainAndPath(site).ifBlank { site.name.orEmpty() }
        }
    }

    data class ViewState(
        val siteAddress: String,
        val steps: List<Step>
    ) {
        val isDone = steps.all { it.state == StepState.Success }
        val failedStep = steps.firstOrNull { it.state is StepState.Error }
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

    enum class ConnectStoreStage {
        FetchStatus,
        WebViewConnection,
        ConnectAccount,
        ConfirmConnection
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

    data class ShowJetpackConnectionWebView(
        val url: String
    ) : Event()

    companion object {
        private const val ERROR_CODE_FORBIDDEN = 403

        @VisibleForTesting
        internal const val KEY_CURRENT_STEP = "woo-push-connection-current-step"

        @VisibleForTesting
        internal const val KEY_CONNECT_STORE_STAGE = "woo-push-connection-connect-store-stage"

        @VisibleForTesting
        internal const val KEY_PENDING_CONNECTION_STATUS = "woo-push-connection-pending-status"
    }
}
