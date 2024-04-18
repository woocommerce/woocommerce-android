package com.woocommerce.wear.ui.login

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class LoginViewModel @AssistedInject constructor (
    private val loginRepository: LoginRepository,
    @Assisted private val navController: NavHostController
) : ViewModel() {
    fun onLoginButtonClicked() {
        loginRepository.apply {  }
        navController.navigate("myStore")
    }

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): LoginViewModel
    }
}
