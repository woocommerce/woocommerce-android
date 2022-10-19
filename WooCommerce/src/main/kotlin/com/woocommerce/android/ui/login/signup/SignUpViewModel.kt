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
            _viewState.value = SignUpState(isLoading = true)
            signUpRepository.createAccount(email, password)
                .fold(
                    onFailure = { _viewState.value = SignUpState(isError = true, errorMessage = it.message) },
                    onSuccess = {
                        _viewState.value = SignUpState(isLoading = false)
                        triggerEvent(NavigateToNextStep)
                    }
                )
        }
    }

    data class SignUpState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val errorMessage: String? = null
    )

    object OnTermsOfServiceClicked : MultiLiveEvent.Event()
    object NavigateToNextStep : MultiLiveEvent.Event()

}
