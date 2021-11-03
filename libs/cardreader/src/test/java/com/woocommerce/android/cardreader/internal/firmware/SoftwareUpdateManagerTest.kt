package com.woocommerce.android.cardreader.internal.firmware

import com.stripe.stripeterminal.external.callable.Cancelable
import com.woocommerce.android.cardreader.internal.connection.BluetoothReaderListenerImpl
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SoftwareUpdateManagerTest {
    private val terminalWrapper: TerminalWrapper = mock()
    private val bluetoothReaderListener: BluetoothReaderListenerImpl = mock()
    private val logWrapper: LogWrapper = mock()
    private val softwareUpdateManager = SoftwareUpdateManager(
        terminalWrapper,
        bluetoothReaderListener,
        logWrapper,
    )

    @Test
    fun `given non null cancel update action, when cancel ongoing update, then action invoked`() {
        // GIVEN
        val cancelUpdateAction: Cancelable = mock()
        whenever(bluetoothReaderListener.cancelUpdateAction).thenReturn(cancelUpdateAction)

        // WHEN
        softwareUpdateManager.cancelOngoingFirmwareUpdate()

        // THEN
        verify(cancelUpdateAction).cancel(any())
    }

    @Test
    fun `given non null cancel update action, when cancel ongoing update, then action nullified`() {
        // GIVEN
        val cancelUpdateAction: Cancelable = mock()
        whenever(bluetoothReaderListener.cancelUpdateAction).thenReturn(cancelUpdateAction)

        // WHEN
        softwareUpdateManager.cancelOngoingFirmwareUpdate()

        // THEN
        verify(bluetoothReaderListener).cancelUpdateAction = null
    }
}
