package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent.SystemBackClicked
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

    @Test
    fun `given info icon is clicked in products screen, when product info dialog is displayed, then ensure dialog heading is correct`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.ProductsDialogInfoIconClicked)
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat((viewModel.state.value as WooPosHomeState.ProductsInfoDialog).header).isEqualTo(
            R.string.woopos_dialog_products_info_heading
        )
    }

    @Test
    fun `given info icon is clicked in products screen, when product info dialog is displayed, then ensure dialog primary message is correct`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.ProductsDialogInfoIconClicked)
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat((viewModel.state.value as WooPosHomeState.ProductsInfoDialog).primaryMessage).isEqualTo(
            R.string.woopos_dialog_products_info_primary_message
        )
    }

    @Test
    fun `given info icon is clicked in products screen, when product info dialog is displayed, then ensure dialog secondary message is correct`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.ProductsDialogInfoIconClicked)
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat((viewModel.state.value as WooPosHomeState.ProductsInfoDialog).secondaryMessage).isEqualTo(
            R.string.woopos_dialog_products_info_secondary_message
        )
    }

    @Test
    fun `given info icon is clicked in products screen, when product info dialog is displayed, then ensure dialog primary button label is correct`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.ProductsDialogInfoIconClicked)
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat((viewModel.state.value as WooPosHomeState.ProductsInfoDialog).primaryButton.label).isEqualTo(
            R.string.woopos_dialog_products_info_button_label
        )
    }

    @Test
    fun `given info icon is clicked in products screen, when product info dialog is displayed, then ensure dialog secondary action label is correct`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.ProductsDialogInfoIconClicked)
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat((viewModel.state.value as WooPosHomeState.ProductsInfoDialog).secondaryMessageActionLabel).isEqualTo(
            R.string.woopos_dialog_products_info_secondary_message_action_label
        )
    }

    @Test
    fun `given product info is displayed, when dialog is dismissed, then ensure the state is updated`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.ProductsDialogInfoIconClicked)
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosHomeUIEvent.DismissProductsInfoDialog)

        // THEN
        assertFalse((viewModel.state.value as WooPosHomeState.ProductsInfoDialog).shouldDisplayDialog)
    }

    private fun createViewModel() = WooPosHomeViewModel(
        childrenToParentEventReceiver,
        parentToChildrenEventSender
    )
}
