package com.woocommerce.android.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

const val TIMEOUT_MILLIS = 20000L
val timeoutFlow: Flow<Boolean>
    get() = flow {
        emit(false)
        delay(TIMEOUT_MILLIS)
        emit(true)
    }

fun <T, R> Flow<T>.combineWithTimeout(
    transform: (data: T, isTimeout: Boolean) -> R
) = combine(this, timeoutFlow, transform)
