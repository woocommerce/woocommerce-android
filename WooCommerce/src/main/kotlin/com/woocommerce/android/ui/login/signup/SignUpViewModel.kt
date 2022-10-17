package com.woocommerce.android.ui.login.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
    private val _viewState = MutableLiveData<SignUpState>()
    val viewState: LiveData<SignUpState> = _viewState

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onTermsOfServiceClicked() {
        triggerEvent(OnTermsOfServiceClicked)
    }

    fun onGetStartedCLicked(email: String, password: String) {
        viewModelScope.launch {
            _viewState.value = Loading
            signUpRepository.createAccount(email, password)
                .fold(
                    onFailure = { _viewState.value = Error },
                    onSuccess = { _viewState.value = AccountCreatedSuccess }
                )
        }
    }

    sealed class SignUpState
    object SignUpForm : SignUpState()
    object Loading : SignUpState()
    object AccountCreatedSuccess : SignUpState()
    object Error : SignUpState()

    object OnTermsOfServiceClicked : MultiLiveEvent.Event()
}
