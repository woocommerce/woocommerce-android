package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Failed
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Logged
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Waiting
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObserveLoginRequest @Inject constructor(
    private val loginRepository: LoginRepository
) {
    operator fun invoke() = combine(
        loginRepository.isUserLoggedIn,
        timeoutWaitingFlow
    ) { isUserLoggedIn, isWaiting ->
        when {
            isUserLoggedIn -> Logged
            isWaiting -> Waiting
            else -> Failed
        }
    }

    private val timeoutWaitingFlow: Flow<Boolean>
        get() = flow {
            emit(true)
            delay(TIMEOUT_MILLIS)
            emit(false)
        }

    enum class LoginRequestState {
        Logged,
        Waiting,
        Failed
    }

    companion object {
        const val TIMEOUT_MILLIS = 20000L
    }
}
