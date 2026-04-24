package com.woocommerce.android.cardreader.remote

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
    }
}
