package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class BluetoothReaderListenerImplTest {
    private val logWrapper: LogWrapper = mock()
    private val listener = BluetoothReaderListenerImpl(logWrapper)

    @Test
    fun `when finishes installing update with error, then failed emitted`() {
        // GIVEN
        val expectedMessage = "message"
        val exception = mock<TerminalException> {
            on { message }.thenReturn(expectedMessage)
        }

        // WHEN
        listener.onFinishInstallingUpdate(mock(), exception)

        // THEN
        assertThat(listener.events.value).isEqualTo(SoftwareUpdateStatus.Failed(expectedMessage))
    }

    @Test
    fun `when finishes installing update without error, then success emitted`() {
        // WHEN
        listener.onFinishInstallingUpdate(mock(), null)

        // THEN
        assertThat(listener.events.value).isEqualTo(SoftwareUpdateStatus.Success)
    }

    @Test
    fun `when on report available update called, then update available emitted`() {
        // WHEN
        listener.onReportAvailableUpdate(mock())

        // THEN
        assertThat(listener.events.value).isEqualTo(SoftwareUpdateAvailability.Available)
    }

    @Test
    fun `when on report reader update prgoress called, then installing emitted`() {
        // GIVEN
        val progress = 0.3f

        // WHEN
        listener.onReportReaderSoftwareUpdateProgress(progress)

        // THEN
        assertThat(listener.events.value).isEqualTo(SoftwareUpdateStatus.Installing(progress))
    }

    @Test
    fun `when on start installing update called, then installation started emitted`() {
        // WHEN
        listener.onStartInstallingUpdate(mock(), mock())

        // THEN
        assertThat(listener.events.value).isEqualTo(SoftwareUpdateStatus.InstallationStarted)
    }
}
