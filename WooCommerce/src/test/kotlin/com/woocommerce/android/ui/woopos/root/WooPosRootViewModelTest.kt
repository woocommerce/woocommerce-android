package com.woocommerce.android.ui.woopos.root

import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.WooPosBottomToolbarState.CardReaderStatus
import com.woocommerce.android.ui.woopos.root.WooPosBottomToolbarState.CardReaderStatus.Unknown
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.skip
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRootViewModelTest : BaseUnitTest() {
    private val cardReaderFacade : WooPosCardReaderFacade = mock()

    @Test
    fun `given reader disconnected, when status button clicked, then should connect` () {
        whenever(cardReaderFacade.readerStatus).thenReturn(flowOf(NotConnected()))
        val sut = createSut()
        assertNotEquals(CardReaderStatus.Connected, sut.bottomToolbarState.value.cardReaderStatus)

        sut.onUiEvent(WooPosRootUIEvent.ConnectToAReaderClicked)

        verify(cardReaderFacade).connectToReader()
    }

    @Test
    fun `given reader connected, when status button clicked, then should not connect` () = testBlocking {
        val cardReader: CardReader = mock()
        whenever(cardReaderFacade.readerStatus).thenReturn(flow { emit(Connected(cardReader)) })
        val sut = createSut()
        val job = launch {
            sut.bottomToolbarState.drop(1).collect {
                assertEquals(CardReaderStatus.Connected, it.cardReaderStatus)
            }
        }

        sut.onUiEvent(WooPosRootUIEvent.ConnectToAReaderClicked)

        verify(cardReaderFacade, never()).connectToReader()
        job.cancel()
    }

    private fun createSut() = WooPosRootViewModel(cardReaderFacade)
}