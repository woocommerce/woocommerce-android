package com.woocommerce.android.ui.login.wpcom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_LOGIN_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_OTP
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NOT_AUTHENTICATED
import javax.inject.Inject

@HiltViewModel
class WPComLogin2FAViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    jetpackAccountRepository: JetpackActivationRepository,
    private val wpComLoginRepository: WPComLoginRepository,
    private val accountRepository: AccountRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val registerDevice: RegisterDevice
) : WPComLoginPostLoginViewModel(
    savedStateHandle,
    selectedSite,
    jetpackAccountRepository,
    analyticsTrackerWrapper,
    registerDevice
) {

    private val navArgs: WPComLogin2FAFragmentArgs by savedStateHandle.navArgs()

    private val isSecurityKeySupported = navArgs.supportedAuthTypes.contains("webauthn")

    private val otp = savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = "", key = "otp")
    private val loadingMessage =
        savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = 0, key = "loading-message")

    private val errorMessage =
        savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = 0, key = "error-message")

    val viewState = combine(
        flowOf(Pair(navArgs.emailOrUsername, navArgs.password)),
        otp,
        errorMessage,
        loadingMessage,
    ) { (emailOrUsername, password), otp, errorMessage, loadingMessage, ->
        ViewState(
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            emailOrUsername = emailOrUsername,
            password = password,
            otp = otp,
            isSecurityKeySupported = isSecurityKeySupported,
            errorMessage = errorMessage.takeIf { it != 0 },
            loadingMessage = loadingMessage.takeIf { it != 0 }
        )
    }.asLiveData()

    fun onCloseClick() {
        triggerEvent(Exit)

        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_VERIFICATION_CODE,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_DISMISS
            )
        )
    }

    fun onSMSLinkClick() = launch {
        loadingMessage.value = R.string.requesting_otp
        wpComLoginRepository.requestTwoStepSMS(
            emailOrUsername = navArgs.emailOrUsername,
            password = navArgs.password
        ).fold(
            onSuccess = { result ->
                when (result) {
                    WPComLoginRepository.SMSRequestResult.UserSignedIn -> fetchAccount()
                    WPComLoginRepository.SMSRequestResult.SMSRequested ->
                        triggerEvent(ShowSnackbar(R.string.requesting_sms_otp_success))
                }
            },
            onFailure = {
                triggerEvent(ShowSnackbar(R.string.requesting_sms_otp_failure))
            }
        )
        loadingMessage.value = 0
    }

    fun onContinueClick() = launch {
        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_VERIFICATION_CODE,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_SUBMIT
            )
        )

        loadingMessage.value = R.string.logging_in
        wpComLoginRepository.submitTwoStepCode(
            emailOrUsername = navArgs.emailOrUsername,
            password = navArgs.password,
            twoStepCode = otp.value
        ).fold(
            onSuccess = { fetchAccount() },
            onFailure = {
                val failure = (it as? OnChangedException)?.error as? AuthenticationError

                when (failure?.type) {
                    INVALID_OTP ->
                        errorMessage.value = R.string.otp_incorrect
                    INCORRECT_USERNAME_OR_PASSWORD, NOT_AUTHENTICATED ->
                        triggerEvent(Exit)
                    else -> {
                        triggerEvent(ShowSnackbar(R.string.error_generic))
                    }
                }

                analyticsTrackerWrapper.track(
                    JETPACK_SETUP_LOGIN_FLOW,
                    mapOf(
                        AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_VERIFICATION_CODE,
                        AnalyticsTracker.KEY_FAILURE to (failure?.type?.name ?: "Unknown error")
                    )
                )
            }
        )
        loadingMessage.value = 0
    }

    fun onSecurityKeyClick() = launch {
        if (navArgs.webauthnNonce.isBlank()) {
            triggerEvent(ShowSnackbar(R.string.error_generic))
            return@launch
        }
        loadingMessage.value = R.string.logging_in
        wpComLoginRepository.startSecurityKeyChallenge(
            userId = navArgs.userId,
            webauthnNonce = navArgs.webauthnNonce
        ).fold(
            onSuccess = { challengeData ->
                loadingMessage.value = 0
                triggerEvent(
                    StartPasskeyAuthentication(
                        userId = challengeData.userId,
                        twoStepNonce = challengeData.twoStepNonce,
                        challengeJson = challengeData.challengeJson
                    )
                )
            },
            onFailure = {
                loadingMessage.value = 0
                triggerEvent(ShowSnackbar(R.string.error_generic))
            }
        )
    }

    fun onPasskeyResult(userId: String, twoStepNonce: String, clientData: String) = launch {
        loadingMessage.value = R.string.logging_in
        wpComLoginRepository.finishSecurityKeyChallenge(
            userId = userId,
            twoStepNonce = twoStepNonce,
            clientData = clientData
        ).fold(
            onSuccess = { fetchAccount() },
            onFailure = { triggerEvent(ShowSnackbar(R.string.error_generic)) }
        )
        loadingMessage.value = 0
    }

    fun onPasskeyError() {
        triggerEvent(ShowSnackbar(R.string.error_generic))
    }

    private suspend fun fetchAccount() {
        accountRepository.fetchUserAccount().fold(
            onSuccess = {
                onLoginSuccess(navArgs.jetpackStatus)
            },
            onFailure = {
                triggerEvent(ShowSnackbar(R.string.error_fetch_my_profile))
            }
        )
    }

    fun onOTPChanged(enteredOTP: String) {
        errorMessage.value = 0
        this.otp.value = enteredOTP
    }

    data class ViewState(
        val isJetpackInstalled: Boolean,
        val emailOrUsername: String,
        val password: String,
        val otp: String,
        val isSecurityKeySupported: Boolean = false,
        val errorMessage: Int? = null,
        val loadingMessage: Int? = null
    ) {
        val enableSubmit = otp.isNotBlank()
    }

    data class StartPasskeyAuthentication(
        val userId: String,
        val twoStepNonce: String,
        val challengeJson: String
    ) : MultiLiveEvent.Event()
}
