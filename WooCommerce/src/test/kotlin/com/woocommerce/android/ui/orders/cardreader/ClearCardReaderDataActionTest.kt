package com.woocommerce.android.ui.orders.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ClearCardReaderDataActionTest : BaseUnitTest() {
    private val cardReaderManager: CardReaderManager = mock()

    private val sut = ClearCardReaderDataAction(cardReaderManager)

    @Test
    fun `Given card reader is initialised, when clearing card reader data, cache is cleared`() =
        testBlocking {
            whenever(cardReaderManager.initialized).thenReturn(true)

            sut.invoke()

            verify(cardReaderManager).clearCachedCredentials()
            verify(cardReaderManager).disconnectReader()
        }

    @Test
    fun `Given card reader is initialised, when clearing card reader data, card reader is disconnected`() =
        testBlocking {
            whenever(cardReaderManager.initialized).thenReturn(true)

            sut.invoke()

            verify(cardReaderManager).disconnectReader()
        }

    @Test
    fun `Given card reader not initialised, when clearing card reader data, nothing happens`() = testBlocking {
        whenever(cardReaderManager.initialized).thenReturn(false)

        sut.invoke()

        verify(cardReaderManager, never()).clearCachedCredentials()
        verify(cardReaderManager, never()).disconnectReader()
    }
}
