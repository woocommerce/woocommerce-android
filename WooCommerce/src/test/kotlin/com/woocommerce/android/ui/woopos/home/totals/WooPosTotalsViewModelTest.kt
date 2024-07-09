package com.woocommerce.android.ui.woopos.home.totals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosTotalsViewModelTest : BaseUnitTest() {
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val cardReaderFacade: WooPosCardReaderFacade = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val priceFormat: WooPosFormatPrice = mock()

    private fun createMockSavedStateHandle(): SavedStateHandle {
        return SavedStateHandle(
            mapOf(
                "orderId" to EMPTY_ORDER_ID,
                "totalsViewState" to WooPosTotalsState.Loading
            )
        )
    }

    private companion object {
        private const val EMPTY_ORDER_ID = -1L
    }

    @Test
    fun `initial state is loading`() = runTest {
        // WHEN
        val savedState = createMockSavedStateHandle()
        val viewModel = createViewModel(savedState)

        // THEN
        assertEquals(WooPosTotalsState.Loading, viewModel.state.value)
    }

    @Test
    fun `given OnNewTransactionClicked, should send NewTransactionClicked event and reset state to initial`() = runTest {
        // GIVEN
        val savedState = createMockSavedStateHandle()
        val viewModel = createViewModel(savedState)

        // WHEN
        viewModel.onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked)

        // THEN
        assertEquals(WooPosTotalsState.Loading, viewModel.state.value)
        verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NewTransactionClicked)
    }

    @Test
    fun `given OrderCreationStarted event, should reset state to initial`() = runTest {
        // GIVEN
        val savedState = createMockSavedStateHandle()
        val parentToChildrenEventsFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsFlow)

        val viewModel = createViewModel(savedState)

        // WHEN
        parentToChildrenEventsFlow.emit(ParentToChildrenEvent.OrderCreation.OrderCreationStarted)

        // THEN
        assertEquals(WooPosTotalsState.Loading, viewModel.state.value)
    }

    private fun createViewModel(savedState: SavedStateHandle) = WooPosTotalsViewModel(
        parentToChildrenEventReceiver,
        childrenToParentEventSender,
        cardReaderFacade,
        orderDetailRepository,
        priceFormat,
        savedState
    )
}
