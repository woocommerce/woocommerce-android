package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Failed
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Logged
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Waiting
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ObserveLoginRequest @Inject constructor(
    private val loginRepository: LoginRepository
) {
    operator fun invoke() = combine(
        loginRepository.isUserLoggedIn,
        timeoutFlow
    ) { isUserLoggedIn, timeoutState ->
        when {
            isUserLoggedIn -> Logged
            timeoutState == LoginTimeoutState.Waiting -> Waiting
            else -> Failed
        }
    }

    private val timeoutFlow: Flow<LoginTimeoutState>
        get() = flow {
            emit(LoginTimeoutState.Waiting)
            delay(TIMEOUT_MILLIS)
            emit(LoginTimeoutState.Timeout)
        }

    enum class LoginRequestState {
        Logged,
        Waiting,
        Failed
    }

    enum class LoginTimeoutState {
        Waiting,
        Timeout
    }

    companion object {
        const val TIMEOUT_MILLIS = 20000L
    }
}
