package com.woocommerce.android.cardreader.internal

import android.app.Application
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.stripeterminal.TerminalLifecycleObserver
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CardReaderManagerImplTest {
    private lateinit var cardReaderManager: CardReaderManagerImpl
    private val terminalWrapper: TerminalWrapper = mock()
    private val tokenProvider: TokenProvider = mock()
    private val lifecycleObserver: TerminalLifecycleObserver = mock()
    private val application: Application = mock()

    @Before
    fun setUp() {
        cardReaderManager = CardReaderManagerImpl(terminalWrapper, tokenProvider)
        whenever(terminalWrapper.getLifecycleObserver()).thenReturn(lifecycleObserver)
    }

    @Test
    fun `when terminal is not initialized, then memory not trimmed`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        cardReaderManager.onTrimMemory(0)

        verify(lifecycleObserver, never()).onTrimMemory(anyInt(), any())
    }

    @Test
    fun `when terminal is initialized, then memory gets trimmed`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)
        cardReaderManager.initialize(application)
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        cardReaderManager.onTrimMemory(0)

        verify(lifecycleObserver, times(1)).onTrimMemory(anyInt(), any())
    }

    @Test
    fun `when manager gets initialized, then terminal gets registered to activity lifecycle`() {
        cardReaderManager.initialize(application)

        verify(application, atLeastOnce()).registerActivityLifecycleCallbacks(lifecycleObserver)
    }

    @Test
    fun `when terminal is initialized, then isInitialized returns true`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(true)

        assertThat(cardReaderManager.isInitialized()).isTrue()
    }

    @Test
    fun `when terminal is not initialized, then isInitialized returns false`() {
        whenever(terminalWrapper.isInitialized()).thenReturn(false)

        assertThat(cardReaderManager.isInitialized()).isFalse()
    }
}
