package com.woocommerce.android.cardreader.internal

import android.app.Application
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.SpecificReader
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
import com.woocommerce.android.cardreader.internal.connection.TerminalListenerImpl
import com.woocommerce.android.cardreader.internal.firmware.SoftwareUpdateManager
import com.woocommerce.android.cardreader.internal.payments.InteracRefundManager
import com.woocommerce.android.cardreader.internal.payments.PaymentManager
import com.woocommerce.android.cardreader.internal.wrappers.TerminalApplicationDelegateWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.RefundParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class CardReaderManagerImplTest : CardReaderBaseUnitTest() {
    private lateinit var cardReaderManager: CardReaderManagerImpl
    private val terminalApplicationDelegateWrapper: TerminalApplicationDelegateWrapper = mock()
    private val terminalWrapper: TerminalWrapper = mock {
        on { getLifecycleObserver() }.thenReturn(terminalApplicationDelegateWrapper)
    }
    private val tokenProvider: TokenProvider = mock()
    private val application: Application = mock()
    private val logWrapper: LogWrapper = mock()
    private val paymentManager: PaymentManager = mock()
    private val interacRefundManager: InteracRefundManager = mock()
    private val connectionManager: ConnectionManager = mock()
    private val softwareUpdateManager: SoftwareUpdateManager = mock()
    private val terminalListener: TerminalListenerImpl = mock()

    private val supportedReaders =
        CardReaderTypesToDiscover.SpecificReaders(listOf(SpecificReader.Chipper2X, SpecificReader.StripeM2))

    private val locationId = "locationId"

    @Before
    fun setUp() {
        cardReaderManager = CardReaderManagerImpl(
            application,
            terminalWrapper,
            tokenProvider,
            logWrapper,
            paymentManager,
            interacRefundManager,
            connectionManager,
            softwareUpdateManager,
            terminalListener,
        )
    }

    @Test
    fun `when manager gets initialized, then terminal gets registered to components lifecycle`() {
        cardReaderManager.initialize()

        verify(application, atLeastOnce()).registerComponentCallbacks(any())
    }

    @Test
    fun `given application delegate, when manager gets initialized, then delegate calls on create`() {
        cardReaderManager.initialize()

        verify(terminalApplicationDelegateWrapper).onCreate(application)
    }

    @Test
    fun `when terminal is initialized, then isInitialized returns true`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        assertThat(cardReaderManager.initialized).isTrue()
    }

    @Test
    fun `when terminal is not initialized, then isInitialized returns false`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        assertThat(cardReaderManager.initialized).isFalse()
    }

    @Test
    fun `given terminal not initialized, when init() invoked, then Terminal init() invoked`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        cardReaderManager.initialize()

        verify(terminalWrapper).initTerminal(any(), any(), any(), any())
    }

    @Test
    fun `given terminal initialized, when init() invoked, then Terminal init() not invoked`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        cardReaderManager.initialize()

        verify(terminalWrapper, never()).initTerminal(any(), any(), any(), any())
    }

    @Test(expected = IllegalStateException::class)
    fun `given terminal not initialized, when reader discovery started, then exception is thrown`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        cardReaderManager.discoverReaders(true, supportedReaders)
    }

    @Test(expected = IllegalStateException::class)
    fun `given terminal not initialized, when connecting to reader started, then exception is thrown`() =
        testBlocking {
            whenever(terminalWrapper.isInitialized()).thenReturn(false)

            cardReaderManager.startConnectionToReader(mock(), locationId)
        }

    @Test
    fun `given terminal not initialized when disconnect from reader, then exception is thrown`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        assertThatIllegalStateException().isThrownBy {
            testBlocking {
                cardReaderManager.disconnectReader()
            }
        }
    }

    @Test
    fun `given terminal initialized and no connected reader when disconnect from reader then return false`() =
        testBlocking {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)
            whenever(terminalWrapper.getConnectedReader()).thenReturn(null)

            assertThat(cardReaderManager.disconnectReader()).isFalse()
        }

    @Test
    fun `given terminal initialized and connected reader and success when disconnect from reader then return true`() =
        testBlocking {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)
            whenever(terminalWrapper.getConnectedReader()).thenReturn(mock())
            whenever(connectionManager.disconnectReader()).thenReturn(true)

            assertThat(cardReaderManager.disconnectReader()).isTrue()
        }

    @Test
    fun `given terminal initialized and connected reader and fail when disconnect from reader then return false`() =
        testBlocking {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)
            whenever(terminalWrapper.getConnectedReader()).thenReturn(mock())
            whenever(connectionManager.disconnectReader()).thenReturn(false)

            assertThat(cardReaderManager.disconnectReader()).isFalse()
        }

    @Test(expected = IllegalStateException::class)
    fun `given terminal not initialized, when installing software update, then exception is thrown`() =
        testBlocking {
            whenever(terminalWrapper.isInitialized()).thenReturn(false)

            cardReaderManager.startAsyncSoftwareUpdate()
        }

    @Test
    fun `given terminal is initialized, when installing software update, updateSoftware is called`() =
        testBlocking {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)

            cardReaderManager.startAsyncSoftwareUpdate()

            verify(softwareUpdateManager).startAsyncSoftwareUpdate()
        }

    @Test
    fun `when cancel ongoing update, then update manager called`() {
        cardReaderManager.cancelOngoingFirmwareUpdate()

        verify(softwareUpdateManager).cancelOngoingFirmwareUpdate()
    }

    @Test
    fun `when collect payment is initiated, then reset bluetooth card reader messages`() =
        testBlocking {
            cardReaderManager.collectPayment(mock())

            verify(connectionManager).resetBluetoothCardReaderDisplayMessage()
        }

    // Interac Refund tests

    @Test
    fun `given terminal not initialized when interac refund, then exception is thrown`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        assertThatIllegalStateException().isThrownBy {
            testBlocking {
                cardReaderManager.refundInteracPayment(mock())
            }
        }
    }

    @Test
    fun `when refund interac payment is initiated, then reset bluetooth card reader messages`() =
        testBlocking {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)
            cardReaderManager.refundInteracPayment(mock())

            verify(connectionManager).resetBluetoothCardReaderDisplayMessage()
        }

    @Test
    fun `when refund interac payment is initiated, then refund interac payment is called with correct params`() =
        testBlocking {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)
            val refundParams = RefundParams(
                chargeId = "chargeId",
                amount = BigDecimal.TEN,
                currency = "USD"
            )
            val captor = argumentCaptor<RefundParams>()

            cardReaderManager.refundInteracPayment(refundParams)

            verify(interacRefundManager).refundInteracPayment(captor.capture())
            assertThat(captor.firstValue).isEqualTo(refundParams)
        }
}
