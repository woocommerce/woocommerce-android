package com.woocommerce.android.cardreader.internal

import android.app.Application
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
import com.woocommerce.android.cardreader.internal.connection.TerminalListenerImpl
import com.woocommerce.android.cardreader.internal.firmware.SoftwareUpdateManager
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CardReaderManagerImplTest {
    private lateinit var cardReaderManager: CardReaderManagerImpl
    private val terminalWrapper: TerminalWrapper = mock()
    private val tokenProvider: TokenProvider = mock()
    private val application: Application = mock()
    private val logWrapper: LogWrapper = mock()
    private val connectionManager: ConnectionManager = mock()
    private val softwareUpdateManager: SoftwareUpdateManager = mock()
    private val terminalListener: TerminalListenerImpl = mock()

    @Before
    fun setUp() {
        cardReaderManager = CardReaderManagerImpl(
            terminalWrapper,
            tokenProvider,
            logWrapper,
            mock(),
            connectionManager,
            softwareUpdateManager,
            terminalListener,
        )
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

        cardReaderManager.discoverReaders(true)
    }

    @Test(expected = IllegalStateException::class)
    fun `given terminal not initialized, when connecting to reader started, then exception is thrown`() =
        runBlockingTest {
            whenever(terminalWrapper.isInitialized()).thenReturn(false)

            cardReaderManager.connectToReader(mock())
        }

    @Test
    fun `given terminal not initialized when disconnect from reader, then exception is thrown`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        assertThatIllegalStateException().isThrownBy {
            runBlockingTest {
                cardReaderManager.disconnectReader()
            }
        }
    }

    @Test
    fun `given terminal initialized and no connected reader when disconnect from reader then return false`() =
        runBlockingTest {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)
            whenever(terminalWrapper.getConnectedReader()).thenReturn(null)

            assertThat(cardReaderManager.disconnectReader()).isFalse()
        }

    @Test
    fun `given terminal initialized and connected reader and success when disconnect from reader then return true`() =
        runBlockingTest {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)
            whenever(terminalWrapper.getConnectedReader()).thenReturn(mock())
            whenever(connectionManager.disconnectReader()).thenReturn(true)

            assertThat(cardReaderManager.disconnectReader()).isTrue()
        }

    @Test
    fun `given terminal initialized and connected reader and fail when disconnect from reader then return false`() =
        runBlockingTest {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)
            whenever(terminalWrapper.getConnectedReader()).thenReturn(mock())
            whenever(connectionManager.disconnectReader()).thenReturn(false)

            assertThat(cardReaderManager.disconnectReader()).isFalse()
        }

    @Test(expected = IllegalStateException::class)
    fun `given terminal not initialized, when installing software update, then exception is thrown`() =
        runBlockingTest {
            whenever(terminalWrapper.isInitialized()).thenReturn(false)

            cardReaderManager.installSoftwareUpdate()
        }

    @Test
    fun `given terminal is initialized, when installing software update, installSoftwareUpdate is called`() =
        runBlockingTest {
            whenever(terminalWrapper.isInitialized()).thenReturn(true)

            cardReaderManager.installSoftwareUpdate()

            verify(terminalWrapper).installSoftwareUpdate()
        }
}
