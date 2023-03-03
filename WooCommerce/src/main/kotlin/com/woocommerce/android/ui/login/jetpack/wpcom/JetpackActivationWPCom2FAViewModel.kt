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
    val viewState = combine(
        flowOf(navArgs.emailOrUsername),
        flowOf(navArgs.password),
        otp,
    ) { emailOrUsername, password, otp ->
        ViewState(
            emailOrUsername = emailOrUsername,
            password = password,
            otp = otp,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled
        )
    }.asLiveData()

    fun onCloseClick() {
        triggerEvent(Exit)
    }

    fun onSMSLinkClick() {
        triggerEvent(ShowSnackbar(R.string.requesting_otp))
    }

    fun onContinueClick() = launch {
        wpComLoginRepository.submitTwoStepCode(
            emailOrUsername = navArgs.emailOrUsername,
            password = navArgs.password,
            twoStepCode = otp.value
        ).fold(
            onSuccess = { fetchAccount() },
            onFailure = { triggerEvent(ShowSnackbar(R.string.error_generic)) }
        )
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
        val isJetpackInstalled: Boolean
    )
}
