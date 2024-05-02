package com.woocommerce.android.ui.login

import android.util.Log
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Logged
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Timeout
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
        loginRepository.isSiteAvailable,
        timeoutWaitingFlow
    ) { isSiteAvailable, isWaiting ->
        when {
            isSiteAvailable -> Logged
            isWaiting -> Waiting
            else -> Timeout
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
        Timeout
    }

    companion object {
        const val TIMEOUT_MILLIS = 20000L
    }
}
