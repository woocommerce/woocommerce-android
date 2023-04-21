package com.woocommerce.android.ui.login.signup

import androidx.annotation.StringRes
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.SIGNUP_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker
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
import com.woocommerce.android.util.StringUtils.isValidEmail
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
    private companion object {
        const val PASSWORD_MIN_LENGTH = 7
    }

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
        AnalyticsTracker.track(stat = AnalyticsEvent.SIGNUP_LOGIN_BUTTON_TAPPED)
        triggerEvent(OnLoginClicked)
    }

    fun onEmailContinueClicked(email: String) {
        val trimmedEmail = email.trim()
        _viewState.value = _viewState.value?.copy(email = trimmedEmail)
        if (isValidEmail(trimmedEmail)) {
            launch {
                _viewState.value = _viewState.value?.copy(error = null, isLoading = true)
                val result = signUpRepository.createAccount(email, "")
                if (result is AccountCreationError && result.error == EMAIL_EXIST) {
                    triggerEvent(OnEmailAlreadyExistError(trimmedEmail))
                } else _viewState.value = _viewState.value?.copy(
                    stepType = SignUpStepType.PASSWORD,
                    error = null
                )
                _viewState.value = _viewState.value?.copy(isLoading = false)
            }
        } else {
            _viewState.value = _viewState.value?.copy(
                isLoading = false,
                error = SignUpErrorUi(
                    type = EMAIL,
                    stringId = R.string.signup_email_invalid_input
                )
            )
        }
    }

    fun onPasswordContinueClicked(inputPassword: String) {
        AnalyticsTracker.track(stat = AnalyticsEvent.SIGNUP_SUBMITTED)

        validatePassword(inputPassword)?.let { error ->
            _viewState.value = _viewState.value?.copy(error = error.toSignUpErrorUi())
            return
        }

        if (!networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(org.wordpress.android.mediapicker.source.wordpress.R.string.no_network_message))
            return
        }

        _viewState.value = _viewState.value?.copy(
            password = inputPassword,
            isLoading = true
        )

        _viewState.value?.let { state ->
            val email = state.email!!
            val password = state.password!!

            launch {
                when (val result = signUpRepository.createAccount(email, password)) {
                    is AccountCreationError -> {
                        AnalyticsTracker.track(
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

    private fun validatePassword(password: String): SignUpError? = when {
        password.length < PASSWORD_MIN_LENGTH -> PASSWORD_TOO_SHORT
        password.isDigitsOnly() -> PASSWORD_INVALID
        else -> null
    }

    data class SignUpState(
        val stepType: SignUpStepType,
        val email: String? = null,
        val password: String? = null,
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
    object OnLoginClicked : MultiLiveEvent.Event()
    data class OnEmailAlreadyExistError(val email: String) : MultiLiveEvent.Event()
}
