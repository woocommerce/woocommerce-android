package com.woocommerce.android.cardreader.internal.connection

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.DiscoveryListener
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTED
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTING
import com.stripe.stripeterminal.model.external.ConnectionStatus.NOT_CONNECTED
import com.stripe.stripeterminal.model.external.Reader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderStatus
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ConnectionManagerTest {
    private val terminalWrapper: TerminalWrapper = mock()
    private val logWrapper: LogWrapper = mock()

    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setUp() {
        connectionManager = ConnectionManager(terminalWrapper, logWrapper)
    }

    @Test
    fun `when reader discovery started, then observers get notified`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        connectionManager.startDiscovery(true)

        Assertions.assertThat(connectionManager.discoveryEvents.value).isEqualTo(CardReaderDiscoveryEvents.Started)
    }

    @Test
    fun `when readers discovered, then observers get notified`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)
        val dummyReaderId = "12345"
        val discoveredReaders = listOf(
            mock<Reader>()
                .apply { whenever(serialNumber).thenReturn(dummyReaderId) })
        whenever(terminalWrapper.discoverReaders(any(), any(), any()))
            .thenAnswer {
                it.getArgument<DiscoveryListener>(1).onUpdateDiscoveredReaders(discoveredReaders)
                mock<Cancelable>()
            }

        connectionManager.startDiscovery(true)

        Assertions.assertThat(connectionManager.discoveryEvents.value).isEqualTo(
            CardReaderDiscoveryEvents.ReadersFound(listOf(dummyReaderId))
        )
    }

    @Test
    fun `when reader unexpectedly disconnected, then observers get notified`() {
        connectionManager.onUnexpectedReaderDisconnect(mock())

        Assertions.assertThat(connectionManager.readerStatus.value).isEqualTo(
            CardReaderStatus.NOT_CONNECTED
        )
    }

    @Test
    fun `when reader disconnected, then observers get notified`() {
        connectionManager.onConnectionStatusChange(NOT_CONNECTED)

        Assertions.assertThat(connectionManager.readerStatus.value).isEqualTo(
            CardReaderStatus.NOT_CONNECTED
        )
    }

    @Test
    fun `when connecting to reader, then observers get notified`() {
        connectionManager.onConnectionStatusChange(CONNECTING)

        Assertions.assertThat(connectionManager.readerStatus.value).isEqualTo(
            CardReaderStatus.CONNECTING
        )
    }

    @Test
    fun `when reader connection established, then observers get notified`() {
        connectionManager.onConnectionStatusChange(CONNECTED)

        Assertions.assertThat(connectionManager.readerStatus.value).isEqualTo(
            CardReaderStatus.CONNECTED
        )
    }
}
