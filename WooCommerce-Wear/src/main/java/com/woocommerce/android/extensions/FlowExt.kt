package com.woocommerce.android.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

const val DEFAULT_TIMEOUT_MILLIS = 20000L
private fun createTimeoutFlow(
    timeoutMillis: Long
): Flow<Boolean> = flow {
    emit(false)
    delay(timeoutMillis)
    emit(true)
}

fun <T, R> Flow<T>.combineWithTimeout(
    timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    transform: (data: T, isTimeout: Boolean) -> R
) = combine(this, createTimeoutFlow(timeoutMillis), transform)
