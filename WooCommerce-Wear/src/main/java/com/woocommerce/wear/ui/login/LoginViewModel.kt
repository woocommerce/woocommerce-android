package com.woocommerce.wear.ui.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class LoginViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {
    fun onLoginButtonClicked() {
        // TODO: Implement this function
    }
}
