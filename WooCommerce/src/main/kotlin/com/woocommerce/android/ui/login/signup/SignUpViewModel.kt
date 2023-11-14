package com.woocommerce.android.ui.login.signup

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.SIGNUP_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.login.signup.SignUpFragment.NextStep
import com.woocommerce.android.ui.login.signup.SignUpRepository.AccountCreationError
import com.woocommerce.android.ui.login.signup.SignUpRepository.AccountCreationSuccess
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.EMAIL_EXIST
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.EMAIL_INVALID
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.PASSWORD_INVALID
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.PASSWORD_TOO_SHORT
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.UNKNOWN_ERROR
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError.USERNAME_INVALID
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
    private val appPrefs: AppPrefsWrapper,
    private val signUpCredentialsValidator: SignUpCredentialsValidator,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {
    lateinit var nextStep: NextStep

    private val _viewState = MutableLiveData(SignUpState(stepType = SignUpStepType.EMAIL))
    val viewState: LiveData<SignUpState> = _viewState

    fun onBackPressed() {
        when (_viewState.value?.stepType!!) {
            SignUpStepType.EMAIL -> triggerEvent(MultiLiveEvent.Event.Exit)
            SignUpStepType.PASSWORD -> _viewState.value = _viewState.value?.copy(stepType = SignUpStepType.EMAIL)
        }
    }

    fun onTermsOfServiceClicked() {
        triggerEvent(OnTermsOfServiceClicked)
    }

    fun onLoginClicked() {
        analyticsTrackerWrapper.track(stat = AnalyticsEvent.SIGNUP_LOGIN_BUTTON_TAPPED)
        triggerEvent(OnLoginWithEmail(_viewState.value?.email))
    }

    fun onEmailInputChanged(email: String) {
        _viewState.value = _viewState.value?.copy(
            email = email.trim(),
            error = null
        )
    }

    fun onPasswordInputChanged(password: String) {
        _viewState.value = _viewState.value?.copy(
            password = password,
            error = null
        )
    }

    fun onEmailContinueClicked() {
        val email = _viewState.value!!.email
        if (signUpCredentialsValidator.isEmailValid(email)) {
            if (!networkStatus.isConnected()) {
                triggerEvent(ShowSnackbar(R.string.offline_error))
                return
            }
            launch {
                _viewState.value = _viewState.value?.copy(error = null, isLoading = true)
                // Trigger account creation with empty password to check if the email already exists.
                val result = signUpRepository.createAccount(email, "")
                if (result is AccountCreationError && result.error == EMAIL_EXIST) {
                    triggerEvent(OnLoginWithEmail(email))
                } else {
                    _viewState.value = _viewState.value?.copy(
                        stepType = SignUpStepType.PASSWORD,
                        error = null
                    )
                }
                _viewState.value = _viewState.value?.copy(isLoading = false)
            }
        } else {
            _viewState.value = _viewState.value?.copy(
                error = SignUpErrorUi(
                    type = EMAIL,
                    stringId = R.string.signup_email_invalid_input
                )
            )
        }
    }

    fun onPasswordContinueClicked() {
        analyticsTrackerWrapper.track(stat = AnalyticsEvent.SIGNUP_SUBMITTED)

        _viewState.value?.let { (_, email, password) ->

            signUpCredentialsValidator.validatePassword(password)?.let { error ->
                _viewState.value = _viewState.value?.copy(error = error.toSignUpErrorUi())
                return
            }
            if (!networkStatus.isConnected()) {
                triggerEvent(ShowSnackbar(R.string.offline_error))
                return
            }
            _viewState.value = _viewState.value?.copy(isLoading = true)
            launch {
                when (val result = signUpRepository.createAccount(email, password)) {
                    is AccountCreationError -> {
                        analyticsTrackerWrapper.track(
                            stat = SIGNUP_ERROR,
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
                        appPrefs.setStoreCreationSource(AnalyticsTracker.VALUE_PROLOGUE)
                        analyticsTrackerWrapper.track(stat = AnalyticsEvent.SIGNUP_SUCCESS)
                        _viewState.value = _viewState.value?.copy(isLoading = false)
                        if (nextStep == NextStep.STORE_CREATION) {
                            appPrefs.markAsNewSignUp(true)
                        }
                        triggerEvent(OnAccountCreated)
                    }
                }
            }
        }
    }

    private fun SignUpError.toSignUpErrorUi() =
        when (this) {
            EMAIL_EXIST -> SignUpErrorUi(
                type = EMAIL,
                stringId = R.string.signup_email_exist_input
            )

            EMAIL_INVALID -> SignUpErrorUi(
                type = EMAIL,
                stringId = R.string.signup_email_invalid_input
            )

            PASSWORD_INVALID -> SignUpErrorUi(
                type = PASSWORD,
                stringId = R.string.signup_password_not_secure_enough
            )

            PASSWORD_TOO_SHORT -> SignUpErrorUi(
                type = PASSWORD,
                stringId = R.string.signup_password_too_short
            )

            UNKNOWN_ERROR,
            USERNAME_INVALID -> SignUpErrorUi(
                type = UNKNOWN,
                stringId = R.string.signup_api_generic_error
            )
        }

    data class SignUpState(
        val stepType: SignUpStepType,
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: SignUpErrorUi? = null,
    )

    data class SignUpErrorUi(
        val type: ErrorType,
        @StringRes val stringId: Int
    )

    enum class SignUpStepType {
        EMAIL,
        PASSWORD,
    }

    enum class ErrorType {
        EMAIL,
        PASSWORD,
        UNKNOWN
    }

    object OnTermsOfServiceClicked : MultiLiveEvent.Event()
    object OnAccountCreated : MultiLiveEvent.Event()
    data class OnLoginWithEmail(val email: String?) : MultiLiveEvent.Event()
}
