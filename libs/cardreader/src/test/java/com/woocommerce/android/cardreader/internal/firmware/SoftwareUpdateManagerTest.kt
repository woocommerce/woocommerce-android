package com.woocommerce.android.cardreader.internal.firmware

import com.stripe.stripeterminal.external.callable.Cancelable
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatusErrorType
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.connection.BluetoothReaderListenerImpl
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SoftwareUpdateManagerTest : CardReaderBaseUnitTest() {
    private val terminalWrapper: TerminalWrapper = mock()
    private val bluetoothReaderListener: BluetoothReaderListenerImpl = mock()
    private val logWrapper: LogWrapper = mock()
    private val softwareUpdateManager = SoftwareUpdateManager(
        terminalWrapper,
        bluetoothReaderListener,
        logWrapper,
    )

    @Test
    fun `given update status changes to started, when start update, then function resumes`() = testBlocking {
        // GIVEN
        val availabilityEvents = MutableStateFlow<SoftwareUpdateStatus>(SoftwareUpdateStatus.Unknown)
        whenever(bluetoothReaderListener.updateStatusEvents).thenReturn(availabilityEvents)

        // WHEN
        val asyncJob = launch {
            softwareUpdateManager.startAsyncSoftwareUpdate()
        }

        // THEN
        assertThat(asyncJob.isActive).isTrue()
        availabilityEvents.emit(SoftwareUpdateStatus.InstallationStarted)
        assertThat(asyncJob.isActive).isFalse()
    }

    @Test
    fun `given update status changes to installing, when start update, then function resumes`() = testBlocking {
        // GIVEN
        val availabilityEvents = MutableStateFlow<SoftwareUpdateStatus>(SoftwareUpdateStatus.Unknown)
        whenever(bluetoothReaderListener.updateStatusEvents).thenReturn(availabilityEvents)

        // WHEN
        val asyncJob = launch {
            softwareUpdateManager.startAsyncSoftwareUpdate()
        }

        // THEN
        assertThat(asyncJob.isActive).isTrue()
        availabilityEvents.emit(SoftwareUpdateStatus.Installing(1f))
        assertThat(asyncJob.isActive).isFalse()
    }

    @Test
    fun `given update status changes to success, when start update, then function resumes`() = testBlocking {
        // GIVEN
        val availabilityEvents = MutableStateFlow<SoftwareUpdateStatus>(SoftwareUpdateStatus.Unknown)
        whenever(bluetoothReaderListener.updateStatusEvents).thenReturn(availabilityEvents)

        // WHEN
        val asyncJob = launch {
            softwareUpdateManager.startAsyncSoftwareUpdate()
        }

        // THEN
        assertThat(asyncJob.isActive).isTrue()
        availabilityEvents.emit(SoftwareUpdateStatus.Success)
        assertThat(asyncJob.isActive).isFalse()
    }

    @Test
    fun `given update status changes to failed, when start update, then function resumes`() = testBlocking {
        // GIVEN
        val availabilityEvents = MutableStateFlow<SoftwareUpdateStatus>(SoftwareUpdateStatus.Unknown)
        whenever(bluetoothReaderListener.updateStatusEvents).thenReturn(availabilityEvents)

        // WHEN
        val asyncJob = launch {
            softwareUpdateManager.startAsyncSoftwareUpdate()
        }

        // THEN
        assertThat(asyncJob.isActive).isTrue()
        availabilityEvents.emit(SoftwareUpdateStatus.Failed(SoftwareUpdateStatusErrorType.ServerError, null))
        assertThat(asyncJob.isActive).isFalse()
    }

    @Test
    fun `given update status is install started, when start update, then function resumes`() = testBlocking {
        // GIVEN
        val availabilityEvents = MutableStateFlow<SoftwareUpdateStatus>(SoftwareUpdateStatus.InstallationStarted)
        whenever(bluetoothReaderListener.updateStatusEvents).thenReturn(availabilityEvents)

        // WHEN
        val asyncJob = launch {
            softwareUpdateManager.startAsyncSoftwareUpdate()
        }

        // THEN
        assertThat(asyncJob.isActive).isFalse()
    }

    @Test
    fun `given unknown status and timeout, when start update, then function resumes`() = testBlocking {
        // GIVEN
        val availabilityEvents = MutableStateFlow<SoftwareUpdateStatus>(SoftwareUpdateStatus.Unknown)
        whenever(bluetoothReaderListener.updateStatusEvents).thenReturn(availabilityEvents)

        // WHEN
        val asyncJob = launch {
            softwareUpdateManager.startAsyncSoftwareUpdate()
        }

        // THEN
        assertThat(asyncJob.isActive).isTrue()
        advanceTimeBy(TIMEOUT_LONGER_THAN_UPDATE_STARTED_MS)
        assertThat(asyncJob.isActive).isFalse()
    }

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

    companion object {
        private const val TIMEOUT_LONGER_THAN_UPDATE_STARTED_MS = 30_100L
    }
}
