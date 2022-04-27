package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.SpecificReader
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ConnectionManagerTest : CardReaderBaseUnitTest() {
    private val terminalWrapper: TerminalWrapper = mock()
    private val bluetoothReaderListener: BluetoothReaderListenerImpl = mock()
    private val discoverReadersAction: DiscoverReadersAction = mock()
    private val terminalListenerImpl: TerminalListenerImpl = mock()

    private val supportedReaders =
        CardReaderTypesToDiscover.SpecificReaders(listOf(SpecificReader.Chipper2X, SpecificReader.StripeM2))

    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setUp() {
        connectionManager = ConnectionManager(
            terminalWrapper,
            bluetoothReaderListener,
            discoverReadersAction,
            terminalListenerImpl,
        )
    }

    @Test
    fun `when readers discovered, then observers get notified`() = testBlocking {
        val dummyReaderId = "12345"
        val discoveredReaders = listOf(
            mock<Reader> {
                on { serialNumber }.thenReturn(dummyReaderId)
                on { deviceType }.thenReturn(DeviceType.STRIPE_M2)
            }
        )
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

        val result = connectionManager.discoverReaders(true, supportedReaders).toList()

        assertThat((result.first() as ReadersFound).list.first().id)
            .isEqualTo(dummyReaderId)
    }

    @Test
    fun `given found readers with specified, when readers discovered, then all readers returned`() =
        testBlocking {
            val dummyReaderId = "12345"
            val discoveredReaders = listOf<Reader>(
                mock {
                    on { deviceType }.thenReturn(DeviceType.CHIPPER_2X)
                },
                mock {
                    on { deviceType }.thenReturn(DeviceType.STRIPE_M2)
                },
                mock {
                    on { deviceType }.thenReturn(DeviceType.WISEPOS_E)
                }
            )
            whenever(discoverReadersAction.discoverReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

            val result = connectionManager.discoverReaders(true, supportedReaders).toList()

            assertThat((result.first() as ReadersFound).list[0].type).isEqualTo(SpecificReader.Chipper2X.name)
            assertThat((result.first() as ReadersFound).list[1].type).isEqualTo(SpecificReader.StripeM2.name)
            assertThat((result.first() as ReadersFound).list.size).isEqualTo(2)
        }

    @Test
    fun `given found readers with unspecified, when readers discovered, then required readers returned`() =
        testBlocking {
            val dummyReaderId = "12345"
            val discoveredReaders = listOf<Reader>(
                mock {
                    on { deviceType }.thenReturn(DeviceType.CHIPPER_2X)
                },
                mock {
                    on { deviceType }.thenReturn(DeviceType.STRIPE_M2)
                },
                mock {
                    on { deviceType }.thenReturn(DeviceType.WISEPOS_E)
                }
            )
            whenever(discoverReadersAction.discoverReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

            val result = connectionManager.discoverReaders(
                true,
                CardReaderTypesToDiscover.UnspecifiedReaders
            ).toList()

            assertThat((result.first() as ReadersFound).list[0].type).isEqualTo(SpecificReader.Chipper2X.name)
            assertThat((result.first() as ReadersFound).list[1].type).isEqualTo(SpecificReader.StripeM2.name)
            assertThat((result.first() as ReadersFound).list[2].type).isEqualTo(SpecificReader.WisePadeE.name)
            assertThat((result.first() as ReadersFound).list.size).isEqualTo(3)
        }

    @Test
    fun `given no readers found with specified, when readers discovered, then empty list returned`() =
        testBlocking {
            val discoveredReaders = listOf<Reader>()
            whenever(discoverReadersAction.discoverReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

            val result = connectionManager.discoverReaders(true, supportedReaders).toList()

            assertThat((result.first() as ReadersFound).list).isEmpty()
        }

    @Test
    fun `given no readers found with unspecified, when readers discovered, then empty list returned`() =
        testBlocking {
            val discoveredReaders = listOf<Reader>()
            whenever(discoverReadersAction.discoverReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

            val result = connectionManager.discoverReaders(
                true,
                CardReaderTypesToDiscover.UnspecifiedReaders
            ).toList()

            assertThat((result.first() as ReadersFound).list).isEmpty()
        }

    @Test
    fun `when discovery fails, then observers get notified`() = testBlocking {
        val terminalException = mock<TerminalException>().also { whenever(it.errorMessage).thenReturn("test") }
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(Failure(terminalException)) })

        val result = connectionManager.discoverReaders(true, supportedReaders).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Failed::class.java)
    }

    @Test
    fun `when discovery succeeds, then observers get notified`() = testBlocking {
        whenever(discoverReadersAction.discoverReaders(anyBoolean()))
            .thenReturn(flow { emit(Success) })

        val result = connectionManager.discoverReaders(true, supportedReaders).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Succeeded::class.java)
    }

    @Test
    fun `given reader with location id, when connectToReader, then status updated with connecting`() =
        testBlocking {
            val reader: Reader = mock()
            val cardReader: CardReaderImpl = mock {
                on { cardReader }.thenReturn(reader)
            }
            whenever(terminalWrapper.connectToReader(any(), any(), any(), any())).thenAnswer {
                (it.arguments[2] as ReaderCallback).onFailure(mock())
            }

            connectionManager.startConnectionToReader(cardReader, "location_id")

            verify(terminalListenerImpl).updateReaderStatus(CardReaderStatus.Connecting)
        }

    @Test
    fun `given reader with location id, when connectToReader fails, then status updated with not connected`() =
        testBlocking {
            val reader: Reader = mock()
            val cardReader: CardReaderImpl = mock {
                on { cardReader }.thenReturn(reader)
            }
            val message = "error_message"
            val exception: TerminalException = mock {
                on { errorMessage }.thenReturn(message)
            }
            whenever(terminalWrapper.connectToReader(any(), any(), any(), any())).thenAnswer {
                (it.arguments[2] as ReaderCallback).onFailure(exception)
            }

            connectionManager.startConnectionToReader(cardReader, "location_id")

            verify(terminalListenerImpl).updateReaderStatus(CardReaderStatus.NotConnected(message))
        }

    @Test
    fun `given reader with location id, when connectToReader success, then status updated with connected`() =
        testBlocking {
            val reader: Reader = mock()
            val cardReader: CardReaderImpl = mock {
                on { cardReader }.thenReturn(reader)
            }
            whenever(terminalWrapper.connectToReader(any(), any(), any(), any())).thenAnswer {
                (it.arguments[2] as ReaderCallback).onSuccess(cardReader.cardReader)
            }

            connectionManager.startConnectionToReader(cardReader, "location_id")

            val statusCaptor = argumentCaptor<CardReaderStatus>()
            verify(terminalListenerImpl, times(2)).updateReaderStatus(statusCaptor.capture())
            val connectedStatus = statusCaptor.secondValue as CardReaderStatus.Connected
            assertThat((connectedStatus.cardReader as CardReaderImpl).cardReader).isEqualTo(reader)
        }

    @Test
    fun `when disconnect succeeds, then status updated with not connected`() = testBlocking {
        whenever(terminalWrapper.disconnectReader(any())).thenAnswer {
            (it.arguments[0] as Callback).onSuccess()
        }

        connectionManager.disconnectReader()

        verify(terminalListenerImpl).updateReaderStatus(CardReaderStatus.NotConnected())
    }

    @Test
    fun `when disconnect succeeds, then true is returned`() = testBlocking {
        whenever(terminalWrapper.disconnectReader(any())).thenAnswer {
            (it.arguments[0] as Callback).onSuccess()
        }

        val result = connectionManager.disconnectReader()

        assertThat(result).isTrue()
    }

    @Test
    fun `when disconnect fails, then false is returned`() = testBlocking {
        whenever(terminalWrapper.disconnectReader(any())).thenAnswer {
            (it.arguments[0] as Callback).onFailure(mock())
        }

        val result = connectionManager.disconnectReader()

        assertThat(result).isFalse()
    }

    @Test
    fun `when disconnect fails, then false with not connected`() = testBlocking {
        whenever(terminalWrapper.disconnectReader(any())).thenAnswer {
            (it.arguments[0] as Callback).onFailure(mock())
        }

        connectionManager.disconnectReader()

        verify(terminalListenerImpl).updateReaderStatus(CardReaderStatus.NotConnected())
    }
}
