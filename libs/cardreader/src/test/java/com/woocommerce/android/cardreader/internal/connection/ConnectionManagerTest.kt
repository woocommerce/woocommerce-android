package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ConnectionManagerTest {
    private val terminalWrapper: TerminalWrapper = mock()
    private val bluetoothReaderListener: BluetoothReaderListenerImpl = mock()
    private val discoverReadersAction: DiscoverReadersAction = mock()

    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setUp() {
        connectionManager = ConnectionManager(terminalWrapper, bluetoothReaderListener, discoverReadersAction)
    }

    @Test
    fun `when readers discovered, then observers get notified`() = runBlockingTest {
        val dummyReaderId = "12345"
        val discoveredReaders = listOf(
            mock<Reader>()
                .apply { whenever(serialNumber).thenReturn(dummyReaderId) }
        )
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

        val result = connectionManager.discoverReaders(true).toList()

        assertThat((result.first() as ReadersFound).list.first().id)
            .isEqualTo(dummyReaderId)
    }

    @Test
    fun `when discovery fails, then observers get notified`() = runBlockingTest {
        val terminalException = mock<TerminalException>().also { whenever(it.errorMessage).thenReturn("test") }
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(Failure(terminalException)) })

        val result = connectionManager.discoverReaders(true).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Failed::class.java)
    }

    @Test
    fun `when discovery succeeds, then observers get notified`() = runBlockingTest {
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(Success) })

        val result = connectionManager.discoverReaders(true).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Succeeded::class.java)
    }

    @Test
    fun `given reader with location id, when connectToReader succeeds, then true is returned`() = runBlockingTest {
        val reader: Reader = mock()
        val cardReader: CardReaderImpl = mock {
            on { locationId }.thenReturn("location_id")
            on { cardReader }.thenReturn(reader)
        }
        whenever(terminalWrapper.connectToReader(any(), any(), any(), any())).thenAnswer {
            (it.arguments[2] as ReaderCallback).onSuccess(cardReader.cardReader)
        }
        val result = connectionManager.connectToReader(cardReader)

        assertThat(result).isTrue()
    }

    @Test(expected = IllegalStateException::class)
    fun `given reader without location id, when connectToReader, then IllegalStateException is thrown`() =
        runBlockingTest {
            val reader: Reader = mock()
            val cardReader: CardReaderImpl = mock {
                on { cardReader }.thenReturn(reader)
            }
            whenever(terminalWrapper.connectToReader(any(), any(), any(), any())).thenAnswer {
                (it.arguments[2] as ReaderCallback).onSuccess(cardReader.cardReader)
            }
            connectionManager.connectToReader(cardReader)
        }

    @Test
    fun `given reader with location id, when connectToReader fails, then false is returned`() = runBlockingTest {
        val reader: Reader = mock()
        val cardReader: CardReaderImpl = mock {
            on { locationId }.thenReturn("location_id")
            on { cardReader }.thenReturn(reader)
        }
        whenever(terminalWrapper.connectToReader(any(), any(), any(), any())).thenAnswer {
            (it.arguments[2] as ReaderCallback).onFailure(mock())
        }

        val result = connectionManager.connectToReader(cardReader)

        assertThat(result).isFalse()
    }

    @Test
    fun `when disconnect succeeds, then true is returned`() = runBlockingTest {
        whenever(terminalWrapper.disconnectReader(any())).thenAnswer {
            (it.arguments[0] as Callback).onSuccess()
        }

        val result = connectionManager.disconnectReader()

        assertThat(result).isTrue()
    }

    @Test
    fun `when disconnect fails, then false is returned`() = runBlockingTest {
        whenever(terminalWrapper.disconnectReader(any())).thenAnswer {
            (it.arguments[0] as Callback).onFailure(mock())
        }

        val result = connectionManager.disconnectReader()

        assertThat(result).isFalse()
    }
}
