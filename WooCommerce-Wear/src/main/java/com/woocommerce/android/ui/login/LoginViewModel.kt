package com.woocommerce.android.ui.login

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.phone.PhoneConnectionRepository.RequestState.Waiting
import com.woocommerce.android.ui.NavRoutes
import com.woocommerce.android.ui.NavRoutes.MY_STORE
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
import com.woocommerce.commons.wear.MessagePath.REQUEST_SITE
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@HiltViewModel(assistedFactory = LoginViewModel.Factory::class)
class LoginViewModel @AssistedInject constructor(
    private val loginRepository: LoginRepository,
    private val phoneConnectionRepository: PhoneConnectionRepository,
    @Assisted private val navController: NavHostController,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init { requestSiteData() }

    fun onTryAgainClicked() { requestSiteData() }

    private suspend fun observeLoginChanges() {
        loginRepository.isUserLoggedIn
            .combine(phoneConnectionRepository.stateMachine) { isLoggedIn, currentState ->
                when {
                    isLoggedIn -> LoginState.Logged
                    currentState == Waiting(REQUEST_SITE) -> LoginState.Waiting
                    else -> LoginState.Failed
                }
            }.collect { loginState ->
                when (loginState) {
                    LoginState.Logged -> navController.navigate(MY_STORE.route) {
                        popUpTo(NavRoutes.LOGIN.route) { inclusive = true }
                    }
                    LoginState.Waiting -> _viewState.update { it.copy(isLoading = true) }
                    LoginState.Failed -> _viewState.update { it.copy(isLoading = false) }
                }
            }
    }

    private fun requestSiteData() {
        _viewState.update { it.copy(isLoading = true) }
        launch {
            phoneConnectionRepository.sendMessage(REQUEST_SITE)
                .fold(
                    onSuccess = { observeLoginChanges() },
                    onFailure = { _viewState.update { it.copy(isLoading = false) } }
                )
        }
    }

    enum class LoginState {
        Logged,
        Waiting,
        Failed
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = true
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): LoginViewModel
    }
}
