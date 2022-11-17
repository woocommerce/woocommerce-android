package com.woocommerce.android.ui.login.signup

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.login.signup.SignUpFragment.NextStep
import com.woocommerce.android.ui.login.signup.SignUpRepository.AccountCreationError
import com.woocommerce.android.ui.login.signup.SignUpRepository.AccountCreationSuccess
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError
import com.woocommerce.android.ui.login.signup.SignUpViewModel.ErrorType.EMAIL
import com.woocommerce.android.ui.login.signup.SignUpViewModel.ErrorType.PASSWORD
import com.woocommerce.android.ui.login.signup.SignUpViewModel.ErrorType.UNKNOWN
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val signUpRepository: SignUpRepository,
    private val networkStatus: NetworkStatus,
    private val appPrefs: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    lateinit var nextStep: NextStep

    private val _viewState = MutableLiveData<SignUpState>()
    val viewState: LiveData<SignUpState> = _viewState

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onTermsOfServiceClicked() {
        triggerEvent(OnTermsOfServiceClicked)
    }

    fun onLoginClicked() {
        AnalyticsTracker.track(stat = AnalyticsEvent.SIGNUP_LOGIN_BUTTON_TAPPED)
        triggerEvent(OnLoginClicked)
    }

    fun onGetStartedCLicked(email: String, password: String) {
        AnalyticsTracker.track(stat = AnalyticsEvent.SIGNUP_SUBMITTED)

        if (!networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(R.string.no_network_message))
            return
        }

        val trimmedEmail = email.trim()
        _viewState.value = SignUpState(email = trimmedEmail, password = password)
        viewModelScope.launch {
            _viewState.value = _viewState.value?.copy(isLoading = true)
            when (val result = signUpRepository.createAccount(trimmedEmail, password)) {
                is AccountCreationError -> {
                    AnalyticsTracker.track(
                        stat = AnalyticsEvent.SIGNUP_ERROR,
                        properties = mapOf(AnalyticsTracker.KEY_ERROR_TYPE to result.error.name)
                    )
                    val error = result.error.toSignUpErrorUi()
                    _viewState.value = _viewState.value?.copy(
                        isLoading = false,
                        error = error
                    )
                    if (error.type == UNKNOWN) {
                        triggerEvent(ShowSnackbar(error.stringId))
                    }
                }
                AccountCreationSuccess -> {
                    AnalyticsTracker.track(stat = AnalyticsEvent.SIGNUP_SUCCESS)
                    _viewState.value = _viewState.value?.copy(isLoading = false)
                    if (nextStep == NextStep.STORE_CREATION) {
                        appPrefs.markAsNewSignUp(true)
                    }
                    triggerEvent(OnAccountCreated)
                }
            }
        }
    }

    private fun SignUpError.toSignUpErrorUi() =
        when (this) {
            SignUpError.EMAIL_EXIST -> SignUpErrorUi(
                type = EMAIL,
                stringId = R.string.signup_email_exist_input
            )
            SignUpError.EMAIL_INVALID -> SignUpErrorUi(
                type = EMAIL,
                stringId = R.string.signup_email_invalid_input
            )
            SignUpError.PASSWORD_INVALID -> SignUpErrorUi(
                type = PASSWORD,
                stringId = R.string.signup_password_not_secure_enough
            )
            SignUpError.PASSWORD_TOO_SHORT -> SignUpErrorUi(
                type = PASSWORD,
                stringId = R.string.signup_password_too_short
            )
            SignUpError.UNKNOWN_ERROR,
            SignUpError.USERNAME_INVALID -> SignUpErrorUi(
                type = UNKNOWN,
                stringId = R.string.signup_api_generic_error
            )
        }

    data class SignUpState(
        val email: String? = null,
        val password: String? = null,
        val isLoading: Boolean = false,
        val error: SignUpErrorUi? = null,
    )

    data class SignUpErrorUi(
        val type: ErrorType,
        @StringRes val stringId: Int
    )

    enum class ErrorType {
        EMAIL,
        PASSWORD,
        UNKNOWN
    }

    object OnTermsOfServiceClicked : MultiLiveEvent.Event()
    object OnAccountCreated : MultiLiveEvent.Event()
    object OnLoginClicked : MultiLiveEvent.Event()
}
