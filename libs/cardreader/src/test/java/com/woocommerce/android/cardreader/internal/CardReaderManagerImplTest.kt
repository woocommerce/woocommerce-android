package com.woocommerce.android.cardreader.internal

import android.app.Application
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.TerminalLifecycleObserver
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
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
    private val connectionManager: ConnectionManager = mock()

    @Before
    fun setUp() {
        cardReaderManager = CardReaderManagerImpl(terminalWrapper, tokenProvider, logWrapper, mock(), connectionManager)
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
}
