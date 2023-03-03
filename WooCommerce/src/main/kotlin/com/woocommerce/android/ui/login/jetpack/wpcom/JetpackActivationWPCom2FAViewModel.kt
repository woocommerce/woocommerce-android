package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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

    val viewState = combine(
        flowOf(navArgs.emailOrUsername),
        flowOf(navArgs.password),
        otp,
        isLoadingDialogShown,
        isSMSRequestDialogShown
    ) { emailOrUsername, password, otp, isLoadingDialogShown, isSMSRequestDialogShown ->
        ViewState(
            emailOrUsername = emailOrUsername,
            password = password,
            otp = otp,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            isLoadingDialogShown = isLoadingDialogShown,
            isSMSRequestDialogShown = isSMSRequestDialogShown
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
            onFailure = { triggerEvent(ShowSnackbar(R.string.error_generic)) }
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
        this.otp.value = enteredOTP
    }

    data class ViewState(
        val emailOrUsername: String,
        val password: String,
        val otp: String,
        val isJetpackInstalled: Boolean,
        val isLoadingDialogShown: Boolean = false,
        val isSMSRequestDialogShown: Boolean = false
    )
}
