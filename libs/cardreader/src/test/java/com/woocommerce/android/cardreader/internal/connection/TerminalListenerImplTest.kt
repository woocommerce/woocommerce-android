package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TerminalListenerImplTest {
    private val terminalWrapper: TerminalWrapper = mock()
    private val logWrapper: LogWrapper = mock()

    private val listener = TerminalListenerImpl(terminalWrapper, logWrapper)

    @Test
    fun `when reader unexpectedly disconnected, then not connected status emitted`() {
        listener.onUnexpectedReaderDisconnect(mock())

        assertThat(listener.readerStatus.value).isEqualTo(CardReaderStatus.NotConnected)
    }

    @Test
    fun `when reader disconnected, then not connected status emitted`() {
        listener.onConnectionStatusChange(ConnectionStatus.NOT_CONNECTED)

        assertThat(listener.readerStatus.value).isEqualTo(CardReaderStatus.NotConnected)
    }

    @Test
    fun `when connecting to reader, then connected status emitted`() {
        listener.onConnectionStatusChange(ConnectionStatus.CONNECTING)

        assertThat(listener.readerStatus.value).isEqualTo(CardReaderStatus.Connecting)
    }

    @Test
    fun `when reader connection established, then connected status emitted`() {
        val cardReader = CardReaderImpl(mock())
        whenever(terminalWrapper.getConnectedReader()).thenReturn(cardReader)
        listener.onConnectionStatusChange(ConnectionStatus.CONNECTED)

        assertThat(listener.readerStatus.value).isEqualTo(CardReaderStatus.Connected(cardReader))
    }
}
