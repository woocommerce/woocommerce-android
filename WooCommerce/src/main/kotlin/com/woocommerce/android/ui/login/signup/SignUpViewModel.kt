package com.woocommerce.android.ui.login.signup

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.signup.SignUpRepository.AccountCreationError
import com.woocommerce.android.ui.login.signup.SignUpRepository.AccountCreationSuccess
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError
import com.woocommerce.android.ui.login.signup.SignUpViewModel.InputFieldError.EMAIL
import com.woocommerce.android.ui.login.signup.SignUpViewModel.InputFieldError.OTHER
import com.woocommerce.android.ui.login.signup.SignUpViewModel.InputFieldError.PASSWORD
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val signUpRepository: SignUpRepository
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val PASSWORD_MIN_LENGTH = 7
    }

    private val _viewState = MutableLiveData<SignUpState>()
    val viewState: LiveData<SignUpState> = _viewState

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onTermsOfServiceClicked() {
        triggerEvent(OnTermsOfServiceClicked)
    }

    fun onGetStartedCLicked(email: String, password: String) {
        if (!areCredentialsValid(email, password)) return
        _viewState.value = SignUpState(email = email, password = password)

        viewModelScope.launch {
            _viewState.value = _viewState.value?.copy(isLoading = true)
            when (val result = signUpRepository.createAccount(email, password)) {
                is AccountCreationError ->
                    _viewState.value = _viewState.value?.copy(
                        isLoading = false,
                        error = result.error.toSignUpErrorUI()
                    )
                AccountCreationSuccess -> {
                    _viewState.value = _viewState.value?.copy(isLoading = false)
                    triggerEvent(NavigateToNextStep)
                }
            }
        }
    }

    private fun areCredentialsValid(email: String, password: String): Boolean {
        // TODO
        return true
    }

    private fun SignUpError.toSignUpErrorUI() =
        when (this) {
            SignUpError.EMAIL_EXIST ->
                SignUpErrorUi(
                    type = EMAIL,
                    stringId = R.string.signup_email_exist_input
                )
            SignUpError.EMAIL_INVALID -> SignUpErrorUi(
                type = EMAIL,
                stringId = R.string.signup_email_invalid_input
            )
            SignUpError.PASSWORD_INVALID -> SignUpErrorUi(
                type = PASSWORD,
                stringId = when {
                    viewState.value?.password.isNullOrEmpty() -> R.string.signup_password_not_secure_enough
                    viewState.value!!.password!!.length < PASSWORD_MIN_LENGTH -> R.string.signup_password_too_short
                    else -> R.string.signup_password_not_secure_enough
                }
            )
            SignUpError.UNKNOWN_ERROR -> SignUpErrorUi(
                type = OTHER,
                stringId = R.string.signup_get_started_button
            )
        }

    data class SignUpState(
        val email: String? = null,
        val password: String? = null,
        val isLoading: Boolean = false,
        val error: SignUpErrorUi? = null,
    )

    data class SignUpErrorUi(
        val type: InputFieldError,
        @StringRes val stringId: Int
    )

    enum class InputFieldError {
        EMAIL,
        PASSWORD,
        OTHER
    }

    object OnTermsOfServiceClicked : MultiLiveEvent.Event()
    object NavigateToNextStep : MultiLiveEvent.Event()

}
