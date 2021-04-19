package com.woocommerce.android.cardreader.internal

import android.app.Application
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.TerminalLifecycleObserver
import com.stripe.stripeterminal.callable.Cancelable
import com.stripe.stripeterminal.callable.DiscoveryListener
import com.stripe.stripeterminal.callable.TerminalListener
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTED
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTING
import com.stripe.stripeterminal.model.external.ConnectionStatus.NOT_CONNECTED
import com.stripe.stripeterminal.model.external.Reader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderStatus
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CardReaderManagerImplTest {
    private lateinit var cardReaderManager: CardReaderManagerImpl
    private val terminalWrapper: TerminalWrapper = mock()
    private val tokenProvider: TokenProvider = mock()
    private val lifecycleObserver: TerminalLifecycleObserver = mock()
    private val application: Application = mock()
    private val logWrapper: LogWrapper = mock()

    @Before
    fun setUp() {
        cardReaderManager = CardReaderManagerImpl(terminalWrapper, tokenProvider, logWrapper, mock())
        whenever(terminalWrapper.getLifecycleObserver()).thenReturn(lifecycleObserver)
    }

    @Test
    fun `when manager gets initialized, then terminal gets registered to activity lifecycle`() {
        cardReaderManager.initialize(application)

        verify(application, atLeastOnce()).registerActivityLifecycleCallbacks(lifecycleObserver)
    }

    @Test
    fun `when manager gets initialized, then terminal gets registered to components lifecycle`() {
        cardReaderManager.initialize(application)

        verify(application, atLeastOnce()).registerComponentCallbacks(any())
    }

    @Test
    fun `when terminal is initialized, then isInitialized returns true`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        assertThat(cardReaderManager.isInitialized).isTrue()
    }

    @Test
    fun `when terminal is not initialized, then isInitialized returns false`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        assertThat(cardReaderManager.isInitialized).isFalse()
    }

    @Test
    fun `given terminal not initialized, when init() invoked, then Terminal init() invoked`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        cardReaderManager.initialize(application)

        verify(terminalWrapper).initTerminal(any(), any(), any(), any())
    }

    @Test
    fun `given terminal initialized, when init() invoked, then Terminal init() not invoked`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        cardReaderManager.initialize(application)

        verify(terminalWrapper, never()).initTerminal(any(), any(), any(), any())
    }

    @Test(expected = IllegalStateException::class)
    fun `given terminal not initialized, when reader discovery started, then exception is thrown`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        cardReaderManager.startDiscovery(true)
    }

    @Test(expected = IllegalStateException::class)
    fun `given terminal not initialized, when connecting to reader started, then exception is thrown`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        cardReaderManager.connectToReader("")
    }

    @Test
    fun `when reader discovery started, then observers get notified`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        cardReaderManager.startDiscovery(true)

        assertThat(cardReaderManager.discoveryEvents.value).isEqualTo(CardReaderDiscoveryEvents.Started)
    }

    @Test
    fun `when readers discovered, then observers get notified`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)
        val dummyReaderId = "12345"
        val discoveredReaders = listOf(mock<Reader>()
            .apply { whenever(serialNumber).thenReturn(dummyReaderId) })
        whenever(terminalWrapper.discoverReaders(any(), any(), any()))
            .thenAnswer {
                it.getArgument<DiscoveryListener>(1).onUpdateDiscoveredReaders(discoveredReaders)
                mock<Cancelable>()
            }

        cardReaderManager.startDiscovery(true)

        assertThat(cardReaderManager.discoveryEvents.value).isEqualTo(
            CardReaderDiscoveryEvents.ReadersFound(listOf(dummyReaderId))
        )
    }

    @Test
    fun `when reader unexpectedly disconnected, then observers get notified`() {
        whenever(terminalWrapper.initTerminal(any(), any(), any(), any()))
            .thenAnswer {
                it.getArgument<TerminalListener>(3).onUnexpectedReaderDisconnect(mock())
            }

        cardReaderManager.initialize(application)

        assertThat(cardReaderManager.readerStatus.value).isEqualTo(
            CardReaderStatus.NOT_CONNECTED
        )
    }

    @Test
    fun `when reader disconnected, then observers get notified`() {
        whenever(terminalWrapper.initTerminal(any(), any(), any(), any()))
            .thenAnswer {
                it.getArgument<TerminalListener>(3).onConnectionStatusChange(NOT_CONNECTED)
            }

        cardReaderManager.initialize(application)

        assertThat(cardReaderManager.readerStatus.value).isEqualTo(
            CardReaderStatus.NOT_CONNECTED
        )
    }

    @Test
    fun `when connecting to reader, then observers get notified`() {
        whenever(terminalWrapper.initTerminal(any(), any(), any(), any()))
            .thenAnswer {
                it.getArgument<TerminalListener>(3).onConnectionStatusChange(CONNECTING)
            }

        cardReaderManager.initialize(application)

        assertThat(cardReaderManager.readerStatus.value).isEqualTo(
            CardReaderStatus.CONNECTING
        )
    }

    @Test
    fun `when reader connection established, then observers get notified`() {
        whenever(terminalWrapper.initTerminal(any(), any(), any(), any()))
            .thenAnswer {
                it.getArgument<TerminalListener>(3).onConnectionStatusChange(CONNECTED)
            }

        cardReaderManager.initialize(application)

        assertThat(cardReaderManager.readerStatus.value).isEqualTo(
            CardReaderStatus.CONNECTED
        )
    }
}
