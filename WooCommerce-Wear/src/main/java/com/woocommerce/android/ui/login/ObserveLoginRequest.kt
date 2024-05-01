package com.woocommerce.android.ui.login

import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.phone.PhoneConnectionRepository.RequestState.Waiting
import com.woocommerce.android.ui.login.LoginViewModel.LoginState
import com.woocommerce.commons.wear.MessagePath.REQUEST_SITE
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

class ObserveLoginRequest @Inject constructor(
    private val loginRepository: LoginRepository,
    private val phoneRepository: PhoneConnectionRepository,
) {
    operator fun invoke() = combine(
        loginRepository.isUserLoggedIn,
        phoneRepository.stateMachine
    ) { isUserLoggedIn, requestState ->
        when {
            isUserLoggedIn -> LoginState.Logged
            requestState == Waiting(REQUEST_SITE) -> LoginState.Waiting
            else -> LoginState.Failed
        }
    }

    enum class LoginRequestState {
        Logged,
        Waiting,
        Failed
    }
}
