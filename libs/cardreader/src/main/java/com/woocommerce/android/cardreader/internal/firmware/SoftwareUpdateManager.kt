package com.woocommerce.android.cardreader.internal.firmware

import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.connection.BluetoothReaderListenerImpl
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

internal class SoftwareUpdateManager(
    private val terminalWrapper: TerminalWrapper,
    private val bluetoothReaderListenerImpl: BluetoothReaderListenerImpl,
    private val logWrapper: LogWrapper,
) {
    suspend fun startAsyncSoftwareUpdate() = coroutineScope {
        val deferred = CompletableDeferred<Unit>()
        val job = launch {
            try {
                withTimeout(timeMillis = UPDATE_STARTED_TIMEOUT_MS) {
                    bluetoothReaderListenerImpl.updateStatusEvents.collect { status ->
                        if (status !is SoftwareUpdateStatus.Unknown) {
                            deferred.complete(Unit)
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                logWrapper.e(LOG_TAG, "update haven't started after $UPDATE_STARTED_TIMEOUT_MS ms")
                deferred.complete(Unit)
            }
        }
        terminalWrapper.installSoftwareUpdate()
        deferred.await()
        job.cancel()
    }

    companion object {
        private const val UPDATE_STARTED_TIMEOUT_MS = 30_000L
    }
}
