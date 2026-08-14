package com.woocommerce.android.cardreader.internal.connection

import android.app.Application
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Failure
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.FoundReaders
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ConnectionManagerTest : CardReaderBaseUnitTest() {

    private val terminalWrapper: TerminalWrapper = mock()
    private val bluetoothReaderListener: BluetoothReaderListenerImpl = mock()
    private val tapToPayReaderListenerImpl: TapToPayReaderListenerImpl = mock()
    private val discoverReadersAction: DiscoverReadersAction = mock()
    private val terminalListenerImpl: TerminalListenerImpl = mock {
        on { readerStatus }.thenReturn(MutableStateFlow(CardReaderStatus.NotConnected()))
    }
    private val application: Application = mock()
    private val logWrapper: LogWrapper = mock()

    private val supportedReaders =
        CardReaderTypesToDiscover.SpecificReaders.ExternalReaders(
            listOf(ReaderType.ExternalReader.Chipper2X, ReaderType.ExternalReader.StripeM2)
        )

    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setUp() {
        val defaultReaderStatus: StateFlow<CardReaderStatus> = MutableStateFlow(CardReaderStatus.NotConnected())
        whenever(terminalListenerImpl.readerStatus).thenReturn(defaultReaderStatus)
        whenever(discoverReadersAction.discoverInternetReaders(anyOrNull(), anyBoolean()))
            .thenReturn(emptyFlow())

        connectionManager = ConnectionManager(
            terminalWrapper,
            bluetoothReaderListener,
            tapToPayReaderListenerImpl,
            discoverReadersAction,
            terminalListenerImpl,
            application,
            logWrapper,
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
        whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
            .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

        val result = connectionManager.discoverReaders(true, supportedReaders).toList()

        assertThat((result.first() as ReadersFound).list.first().id)
            .isEqualTo(dummyReaderId)
    }

    @Test
    fun `given found readers with specified, when readers discovered, then all readers returned`() =
        testBlocking {
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
            whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

            val result = connectionManager.discoverReaders(true, supportedReaders).toList()

            assertThat((result.first() as ReadersFound).list[0].type).isEqualTo(
                ReaderType.ExternalReader.Chipper2X.name
            )
            assertThat((result.first() as ReadersFound).list[1].type).isEqualTo(
                ReaderType.ExternalReader.StripeM2.name
            )
            assertThat((result.first() as ReadersFound).list.size).isEqualTo(2)
        }

    @Test
    fun `given found readers external and built in, when readers discovered, then required readers returned`() =
        testBlocking {
            val discoveredExternalReaders = listOf<Reader>(
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
            val discoveredBuiltInReaders = listOf<Reader>(
                mock {
                    on { deviceType }.thenReturn(DeviceType.TAP_TO_PAY_DEVICE)
                }
            )
            whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredExternalReaders)) })
            whenever(discoverReadersAction.discoverBuildInReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredBuiltInReaders)) })

            val result = connectionManager.discoverReaders(
                true,
                CardReaderTypesToDiscover.UnspecifiedReaders
            ).toList()

            assertThat((result[0] as ReadersFound).list[0].type).isEqualTo(
                ReaderType.BuildInReader.TapToPayDevice.name
            )
            assertThat((result[0] as ReadersFound).list.size).isEqualTo(1)

            assertThat((result[1] as ReadersFound).list[0].type).isEqualTo(
                ReaderType.ExternalReader.Chipper2X.name
            )
            assertThat((result[1] as ReadersFound).list[1].type).isEqualTo(
                ReaderType.ExternalReader.StripeM2.name
            )
            assertThat((result[1] as ReadersFound).list[2].type).isEqualTo(
                ReaderType.ExternalReader.WisePadeE.name
            )
            assertThat((result[1] as ReadersFound).list.size).isEqualTo(3)
        }

    @Test
    fun `given no readers found with specified, when readers discovered, then empty list returned`() =
        testBlocking {
            val discoveredReaders = listOf<Reader>()
            whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

            val result = connectionManager.discoverReaders(true, supportedReaders).toList()

            assertThat((result.first() as ReadersFound).list).isEmpty()
        }

    @Test
    fun `given no readers found with unspecified, when readers discovered, then empty list returned`() =
        testBlocking {
            val discoveredReaders = listOf<Reader>()
            whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })
            whenever(discoverReadersAction.discoverBuildInReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

            val result = connectionManager.discoverReaders(
                true,
                CardReaderTypesToDiscover.UnspecifiedReaders
            ).toList()

            assertThat((result.first() as ReadersFound).list).isEmpty()
        }

    @Test
    fun `given specified buuilt in reader, when reader discovered, then discovered reader returned`() =
        testBlocking {
            val discoveredReaders = listOf<Reader>(
                mock {
                    on { deviceType }.thenReturn(DeviceType.TAP_TO_PAY_DEVICE)
                }
            )
            whenever(discoverReadersAction.discoverBuildInReaders(anyBoolean()))
                .thenReturn(flow { emit(FoundReaders(discoveredReaders)) })

            val result = connectionManager.discoverReaders(
                true,
                CardReaderTypesToDiscover.SpecificReaders.BuiltInReaders(
                    listOf(ReaderType.BuildInReader.TapToPayDevice)
                )
            ).toList()

            assertThat((result.first() as ReadersFound).list[0].type).isEqualTo(
                ReaderType.BuildInReader.TapToPayDevice.name
            )
            assertThat((result.first() as ReadersFound).list.size).isEqualTo(1)
        }

    @Test
    fun `when discovery fails, then observers get notified`() = testBlocking {
        val terminalException = TerminalException(TerminalErrorCode.NOT_CONNECTED_TO_READER, "test")
        whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
            .thenReturn(flow { emit(Failure(terminalException)) })

        val result = connectionManager.discoverReaders(true, supportedReaders).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Failed::class.java)
    }

    @Test
    fun `given TTP unsupported device error, when discovery fails, then ttp specific event emitted`() = testBlocking {
        val terminalException = TerminalException(
            TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
            "Device does not use TEE",
        )
        whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
            .thenReturn(flow { emit(Failure(terminalException)) })

        val result = connectionManager.discoverReaders(true, supportedReaders).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.FailedTapToPayDeviceUnsupported::class.java)
    }

    @Test
    fun `given TTP tampered device error, when discovery fails, then ttp specific event emitted`() = testBlocking {
        val terminalException = TerminalException(TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED, "tampered")
        whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
            .thenReturn(flow { emit(Failure(terminalException)) })

        val result = connectionManager.discoverReaders(true, supportedReaders).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.FailedTapToPayDeviceUnsupported::class.java)
    }

    @Test
    fun `given TTP unsupported android version error, when discovery fails, then ttp specific event emitted`() =
        testBlocking {
            val terminalException = TerminalException(
                TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_ANDROID_VERSION,
                "old android",
            )
            whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
                .thenReturn(flow { emit(Failure(terminalException)) })

            val result = connectionManager.discoverReaders(true, supportedReaders).single()

            assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.FailedTapToPayDeviceUnsupported::class.java)
        }

    @Test
    fun `given non TTP error code, when discovery fails, then generic Failed event emitted`() = testBlocking {
        val terminalException = TerminalException(TerminalErrorCode.NOT_CONNECTED_TO_READER, "other error")
        whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
            .thenReturn(flow { emit(Failure(terminalException)) })

        val result = connectionManager.discoverReaders(true, supportedReaders).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Failed::class.java)
    }

    @Test
    fun `when discovery succeeds, then observers get notified`() = testBlocking {
        whenever(discoverReadersAction.discoverExternalReaders(anyBoolean()))
            .thenReturn(flow { emit(Success) })

        val result = connectionManager.discoverReaders(true, supportedReaders).single()

        assertThat(result).isInstanceOf(CardReaderDiscoveryEvents.Succeeded::class.java)
    }

    @Test
    fun `given reader with location id, when connectToReader, then status updated with connecting`() =
        testBlocking {
            val reader: Reader = mock {
                on { deviceType }.thenReturn(DeviceType.STRIPE_M2)
            }
            val cardReader: CardReaderImpl = mock {
                on { cardReader }.thenReturn(reader)
            }
            whenever(terminalWrapper.connectToReader(any(), any())).thenReturn(mock())

            connectionManager.startConnectionToReader(cardReader, "location_id")

            verify(terminalListenerImpl).updateReaderStatus(CardReaderStatus.Connecting)
        }

    @Test
    fun `given reader with location id, when connectToReader fails, then status updated with not connected and other error code`() =
        testBlocking {
            val reader: Reader = mock {
                on { deviceType }.thenReturn(DeviceType.CHIPPER_2X)
            }
            val cardReader: CardReaderImpl = mock {
                on { cardReader }.thenReturn(reader)
            }
            val message = "error_message"
            val errorCode = TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_READER_ERROR
            val exception: TerminalException = mock {
                on { errorMessage }.thenReturn(message)
                on { this.errorCode }.thenReturn(errorCode)
            }
            whenever(terminalWrapper.connectToReader(any(), any())).thenAnswer { throw exception }

            connectionManager.startConnectionToReader(cardReader, "location_id")

            verify(terminalListenerImpl).updateReaderStatus(
                CardReaderStatus.NotConnected(
                    errorCode = CardReaderStatus.NotConnected.ErrorCode.OTHER,
                    errorMessage = message,
                )
            )
        }

    @Test
    fun `given reader with location id, when connectToReader fails with low batter, then status updated with not connected and batter error code`() =
        testBlocking {
            val reader: Reader = mock {
                on { deviceType }.thenReturn(DeviceType.CHIPPER_2X)
            }
            val cardReader: CardReaderImpl = mock {
                on { cardReader }.thenReturn(reader)
            }
            val message = "error_message"
            val errorCode = TerminalErrorCode.READER_BATTERY_CRITICALLY_LOW
            val exception: TerminalException = mock {
                on { errorMessage }.thenReturn(message)
                on { this.errorCode }.thenReturn(errorCode)
            }
            whenever(terminalWrapper.connectToReader(any(), any())).thenAnswer { throw exception }

            connectionManager.startConnectionToReader(cardReader, "location_id")

            verify(terminalListenerImpl).updateReaderStatus(
                CardReaderStatus.NotConnected(
                    errorCode = CardReaderStatus.NotConnected.ErrorCode.BATTERY_CRITICALLY_LOW,
                    errorMessage = message,
                )
            )
        }

    @Test
    fun `given reader with location id, when connectToReader success, then status updated with connected`() =
        testBlocking {
            val reader: Reader = mock {
                on { deviceType }.thenReturn(DeviceType.STRIPE_M2)
            }
            val cardReader: CardReaderImpl = mock {
                on { cardReader }.thenReturn(reader)
            }
            whenever(terminalWrapper.connectToReader(any(), any())).thenReturn(reader)

            connectionManager.startConnectionToReader(cardReader, "location_id")

            val statusCaptor = argumentCaptor<CardReaderStatus>()
            verify(terminalListenerImpl, times(2)).updateReaderStatus(statusCaptor.capture())
            val connectedStatus = statusCaptor.secondValue as CardReaderStatus.Connected
            assertThat((connectedStatus.cardReader as CardReaderImpl).cardReader).isEqualTo(reader)
        }

    @Test
    fun `when disconnect succeeds, then status updated with not connected`() = testBlocking {
        whenever(terminalWrapper.disconnectReader()).thenReturn(Unit)

        connectionManager.disconnectReader()

        verify(terminalListenerImpl).updateReaderStatus(CardReaderStatus.NotConnected())
    }

    @Test
    fun `when disconnect succeeds, then true is returned`() = testBlocking {
        whenever(terminalWrapper.disconnectReader()).thenReturn(Unit)

        val result = connectionManager.disconnectReader()

        assertThat(result).isTrue()
    }

    @Test
    fun `when disconnect fails, then false is returned`() = testBlocking {
        whenever(terminalWrapper.disconnectReader()).thenAnswer { throw mock<TerminalException>() }

        val result = connectionManager.disconnectReader()

        assertThat(result).isFalse()
    }

    @Test
    fun `when disconnect fails, then false with not connected`() = testBlocking {
        whenever(terminalWrapper.disconnectReader()).thenAnswer { throw mock<TerminalException>() }

        connectionManager.disconnectReader()

        verify(terminalListenerImpl).updateReaderStatus(CardReaderStatus.NotConnected())
    }
}
