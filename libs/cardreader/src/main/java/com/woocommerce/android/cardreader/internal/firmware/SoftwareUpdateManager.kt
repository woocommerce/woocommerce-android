package com.woocommerce.android.cardreader.internal.firmware

import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateInProgress
import com.woocommerce.android.cardreader.internal.connection.BluetoothReaderListenerImpl
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class SoftwareUpdateManager(
    private val terminalWrapper: TerminalWrapper,
    private val bluetoothReaderListenerImpl: BluetoothReaderListenerImpl,
) {
    suspend fun updateSoftware() = coroutineScope {
        val deferred = CompletableDeferred<Unit>()
        val job = launch {
            bluetoothReaderListenerImpl.updateStatusEvents.collect { status ->
                // TODO cardreader Implement timeout to handle if sdk doesn't start the update
                if (status is SoftwareUpdateInProgress) {
                    deferred.complete(Unit)
                }
            }
        }
        terminalWrapper.installSoftwareUpdate()
        deferred.await()
        job.cancel()
    }
}
