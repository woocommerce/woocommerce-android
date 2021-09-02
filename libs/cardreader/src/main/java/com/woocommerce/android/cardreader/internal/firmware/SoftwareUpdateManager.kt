package com.woocommerce.android.cardreader.internal.firmware

import com.woocommerce.android.cardreader.internal.connection.BluetoothReaderListenerImpl
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.suspendCoroutine

internal class SoftwareUpdateManager(
    private val terminalWrapper: TerminalWrapper,
    private val bluetoothReaderListenerImpl: BluetoothReaderListenerImpl,
) {
    suspend fun updateSoftware() {
        suspendCoroutine<Unit> { continuation ->
            terminalWrapper.installSoftwareUpdate()
        }
        bluetoothReaderListenerImpl.updateStatusEvents.collect {
            when (it is)
        }
    }
}
