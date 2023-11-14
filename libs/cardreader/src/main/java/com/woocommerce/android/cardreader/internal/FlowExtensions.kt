package com.woocommerce.android.cardreader.internal

import com.woocommerce.android.cardreader.LogWrapper
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking

internal fun <T> ProducerScope<T>.sendAndLog(status: T, logWrapper: LogWrapper) {
    trySendBlocking(status)
        .onClosed { logWrapper.e(LOG_TAG, it?.message.orEmpty()) }
        .onFailure { logWrapper.e(LOG_TAG, it?.message.orEmpty()) }
}
