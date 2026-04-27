package com.woocommerce.android.cardreader.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

internal const val HEARTBEAT_INTERVAL_MILLIS = 30_000L

internal fun CoroutineScope.launchHeartbeat(connection: CardReaderRemoteConnection): Job = launch {
    while (isActive) {
        delay(HEARTBEAT_INTERVAL_MILLIS)
        runCatching { connection.send(CardReaderRemoteMessage.Ping(UUID.randomUUID().toString())) }
            .onFailure { err ->
                Log.w("CardReaderRemoteHeartbeat", "Ping failed, closing connection: ${err::class.java.simpleName}")
                runCatching { connection.close() }
                return@launch
            }
    }
}
