package com.woocommerce.android.cardreader.internal

import com.nhaarman.mockitokotlin2.mock
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CardReaderManagerImplTest {
    private lateinit var cardReaderManager: CardReaderManagerImpl
    private val terminalWrapper: TerminalWrapper = mock()

    @Before
    fun setUp() {
        cardReaderManager = CardReaderManagerImpl(terminalWrapper)
    }

    @Test
    fun foo() {
    }
}
