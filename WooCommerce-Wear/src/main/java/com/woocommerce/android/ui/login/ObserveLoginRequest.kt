package com.woocommerce.android.ui.login

import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.phone.PhoneConnectionRepository.RequestState
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Failed
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Logged
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Waiting
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
            isUserLoggedIn -> Logged
            requestState == RequestState.Waiting(REQUEST_SITE) -> Waiting
            else -> Failed
        }
    }

    enum class LoginRequestState {
        Logged,
        Waiting,
        Failed
    }
}
