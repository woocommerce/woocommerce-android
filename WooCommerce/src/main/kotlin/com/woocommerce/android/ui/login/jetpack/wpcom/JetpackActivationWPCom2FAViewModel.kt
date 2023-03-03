package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWPCom2FAViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    jetpackAccountRepository: JetpackActivationRepository,
    private val wpComLoginRepository: WPComLoginRepository,
    private val accountRepository: AccountRepository
) : JetpackActivationWPComPostLoginViewModel(savedStateHandle, selectedSite, jetpackAccountRepository) {

    private val navArgs: JetpackActivationWPCom2FAFragmentArgs by savedStateHandle.navArgs()

    private val otp = savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = "", key = "otp")
    private val isLoadingDialogShown = MutableStateFlow(false)
    private val isSMSRequestDialogShown = MutableStateFlow(false)
    private val errorMessage =
        savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = 0, key = "error-message")

    val viewState = combine(
        flowOf(Pair(navArgs.emailOrUsername, navArgs.password)),
        otp,
        errorMessage,
        isLoadingDialogShown,
        isSMSRequestDialogShown
    ) { (emailOrUsername, password), otp, errorMessage, isLoadingDialogShown, isSMSRequestDialogShown ->
        ViewState(
            emailOrUsername = emailOrUsername,
            password = password,
            otp = otp,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            isLoadingDialogShown = isLoadingDialogShown,
            isSMSRequestDialogShown = isSMSRequestDialogShown,
            errorMessage = errorMessage.takeIf { it != 0 }
        )
    }.asLiveData()

    fun onCloseClick() {
        triggerEvent(Exit)
    }

    fun onSMSLinkClick() = launch {
        isSMSRequestDialogShown.value = true
        wpComLoginRepository.requestTwoStepSMS(
            emailOrUsername = navArgs.emailOrUsername,
            password = navArgs.password
        ).fold(
            onSuccess = {
                triggerEvent(ShowSnackbar(R.string.requesting_sms_otp_success))
            },
            onFailure = {
                triggerEvent(ShowSnackbar(R.string.requesting_sms_otp_failure))
            }
        )
        isSMSRequestDialogShown.value = false
    }

    fun onContinueClick() = launch {
        isLoadingDialogShown.value = true
        wpComLoginRepository.submitTwoStepCode(
            emailOrUsername = navArgs.emailOrUsername,
            password = navArgs.password,
            twoStepCode = otp.value
        ).fold(
            onSuccess = { fetchAccount() },
            onFailure = {
                val failure = (it as? OnChangedException)?.error as? AuthenticationError

                when (failure?.type) {
                    AccountStore.AuthenticationErrorType.INVALID_OTP ->
                        errorMessage.value = R.string.otp_incorrect

                    else -> {
                        triggerEvent(ShowSnackbar(R.string.error_generic))
                    }
                }

                triggerEvent(ShowSnackbar(R.string.error_generic))
            }
        )
        isLoadingDialogShown.value = false
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
        val emailOrUsername: String,
        val password: String,
        val otp: String,
        val isJetpackInstalled: Boolean,
        val isLoadingDialogShown: Boolean = false,
        val isSMSRequestDialogShown: Boolean = false,
        val errorMessage: Int? = null
    ) {
        val enableSubmit = otp.isNotBlank()
    }
}
