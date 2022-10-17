package com.woocommerce.android.util

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

suspend fun <T> Flow<T>.runAndCaptureValues(block: suspend () -> Unit): List<T> = coroutineScope {
    val list = mutableListOf<T>()
    val job = launch {
        toList(list)
    }
    block()
    job.cancel()
    return@coroutineScope list
}
