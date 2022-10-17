package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class TerminalListenerImplTest {
    private val logWrapper: LogWrapper = mock()

    private val listener = TerminalListenerImpl(logWrapper)

    @Test
    fun `when reader unexpectedly disconnected, then not connected status emitted`() {
        listener.onUnexpectedReaderDisconnect(mock())

        assertThat(listener.readerStatus.value).isEqualTo(CardReaderStatus.NotConnected())
    }

    @Test
    fun `when reader disconnected, then not connected status emitted`() {
        listener.onConnectionStatusChange(ConnectionStatus.NOT_CONNECTED)

        assertThat(listener.readerStatus.value).isEqualTo(CardReaderStatus.NotConnected())
    }

    @Test
    fun `when update reader status with not connected, then not connected status emitted`() {
        val newStatus = CardReaderStatus.NotConnected()

        listener.updateReaderStatus(newStatus)

        assertThat(listener.readerStatus.value).isEqualTo(newStatus)
    }

    @Test
    fun `when update reader status with connecting, then connecting status emitted`() {
        val newStatus = CardReaderStatus.Connecting

        listener.updateReaderStatus(newStatus)

        assertThat(listener.readerStatus.value).isEqualTo(newStatus)
    }

    @Test
    fun `when update reader status with connected, then connected status emitted`() {
        val newStatus = CardReaderStatus.Connected(mock())

        listener.updateReaderStatus(newStatus)

        assertThat(listener.readerStatus.value).isEqualTo(newStatus)
    }
}
