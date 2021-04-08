package com.woocommerce.android.cardreader.internal.connection

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTED
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTING
import com.stripe.stripeterminal.model.external.ConnectionStatus.NOT_CONNECTED
import com.stripe.stripeterminal.model.external.Reader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.CardReaderStatus
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean

@ExperimentalCoroutinesApi
class ConnectionManagerTest {
    private val terminalWrapper: TerminalWrapper = mock()
    private val logWrapper: LogWrapper = mock()
    private val discoverReadersAction: DiscoverReadersAction = mock()

    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setUp() {
        connectionManager = ConnectionManager(terminalWrapper, logWrapper, discoverReadersAction)
    }

    @Test
    fun `when readers discovered, then observers get notified`() = runBlockingTest {
        val dummyReaderId = "12345"
        val discoveredReaders = listOf(
            mock<Reader>()
                .apply { whenever(serialNumber).thenReturn(dummyReaderId) })
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

        val result = connectionManager.discoverReaders(true).toList()

        Assertions.assertThat(result.first()).isEqualTo(ReadersFound(listOf(dummyReaderId)))
    }

    @Test
    fun `when discovery fails, then observers get notified`() = runBlockingTest {
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(Failure(mock())) })

        val result = connectionManager.discoverReaders(true).single()

        Assertions.assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Failed::class.java)
    }

    @Test
    fun `when discovery succeeds, then observers get notified`() = runBlockingTest {
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(Success) })

        val result = connectionManager.discoverReaders(true).single()

        Assertions.assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Succeeded::class.java)
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
