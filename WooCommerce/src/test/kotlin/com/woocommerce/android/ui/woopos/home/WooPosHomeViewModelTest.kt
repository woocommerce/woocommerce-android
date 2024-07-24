package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent.SystemBackClicked
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WooPosHomeViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val childrenToParentEventReceiver: WooPosChildrenToParentEventReceiver = mock()
    private val parentToChildrenEventSender: WooPosParentToChildrenEventSender = mock()

    @Test
    fun `given state checkout, when SystemBackClicked passed, then BackFromCheckoutToCartClicked event should be sent`() =
        runTest {
            // GIVEN
            whenever(childrenToParentEventReceiver.events).thenReturn(
                flowOf(ChildToParentEvent.CheckoutClicked(emptyList()))
            )
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(SystemBackClicked)

            // THEN
            verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
        }

    @Test
    fun `given state is Cart, when SystemBackClicked passed, then should propagate the event down`() = runTest {
        // GIVEN
        val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
        val viewModel = createViewModel()

        // WHEN
        val result = viewModel.onUIEvent(SystemBackClicked)

        // THEN
        assertFalse(result)
    }

    @Test
    fun `given state checkout is paid, when SystemBackClicked passed, then OrderSuccessfullyPaid event should be sent`() = runTest {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.OrderSuccessfullyPaid)
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(SystemBackClicked)

        // THEN
        verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
    }

    @Test
    fun `when info icon is clicked in products, then display products info dialog`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.ProductsDialogInfoIconClicked)
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertTrue((viewModel.state.value as WooPosHomeState.ProductsInfoDialog).shouldDisplayDialog)
    }

    private fun createViewModel() = WooPosHomeViewModel(
        childrenToParentEventReceiver,
        parentToChildrenEventSender
    )
}
