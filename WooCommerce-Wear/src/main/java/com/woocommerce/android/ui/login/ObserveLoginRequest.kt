package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Logged
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Timeout
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Waiting
import com.woocommerce.android.util.timeoutFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveLoginRequest @Inject constructor(
    private val loginRepository: LoginRepository
) {
    operator fun invoke() = combine(
        loginRepository.isSiteAvailable,
        timeoutFlow
    ) { isSiteAvailable, isTimeout ->
        when {
            isSiteAvailable -> Logged
            isTimeout.not() -> Waiting
            else -> Timeout
        }
    }

    enum class LoginRequestState {
        Logged,
        Waiting,
        Timeout
    }
}
