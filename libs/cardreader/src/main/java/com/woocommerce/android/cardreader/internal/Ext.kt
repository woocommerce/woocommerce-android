package com.woocommerce.android.cardreader.internal

import com.woocommerce.android.cardreader.LogWrapper
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking

fun <T> ProducerScope<T>.sendAndLog(logWrapper: LogWrapper, status: T) {
    trySendBlocking(status)
        .onClosed { logWrapper.e(LOG_TAG, it?.message.orEmpty()) }
        .onFailure { logWrapper.e(LOG_TAG, it?.message.orEmpty()) }
}
