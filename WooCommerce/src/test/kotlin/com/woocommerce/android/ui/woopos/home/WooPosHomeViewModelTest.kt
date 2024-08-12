package com.woocommerce.android.ui.woopos.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class WooPosHomeViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

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
            viewModel.onUIEvent(WooPosHomeUIEvent.SystemBackClicked)

            // THEN
            verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
            assertThat(viewModel.state.value.screenPositionState)
                .isEqualTo(WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty)
        }

    @Test
    fun `given state is Cart, when SystemBackClicked passed, then should show exit confirmation dialog`() = runTest {
        // GIVEN
        val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosHomeUIEvent.SystemBackClicked)

        // THEN
        assertThat(viewModel.state.value.exitConfirmationDialog).isEqualTo(WooPosExitConfirmationDialog)
    }

    @Test
    fun `given state checkout is paid, when SystemBackClicked passed, then OrderSuccessfullyPaid event should be sent`() =
        runTest {
            // GIVEN
            whenever(childrenToParentEventReceiver.events).thenReturn(
                flowOf(ChildToParentEvent.OrderSuccessfullyPaid)
            )
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(WooPosHomeUIEvent.SystemBackClicked)

            // THEN
            verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
            assertThat(viewModel.state.value.screenPositionState)
                .isEqualTo(WooPosHomeState.ScreenPositionState.Cart.Visible.NotEmpty)
        }

    @Test
    fun `given state is Checkout NotPaid, when ExitConfirmationDialogDismissed passed, then exit confirmation dialog should be dismissed`() =
        runTest {
            // GIVEN
            whenever(childrenToParentEventReceiver.events).thenReturn(
                flowOf(ChildToParentEvent.CheckoutClicked(emptyList()))
            )
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed)

            // THEN
            assertThat(viewModel.state.value.exitConfirmationDialog).isNull()
        }

    @Test
    fun `given state is Cart Empty, when ExitPosClicked passed, then exit confirmation dialog should be shown`() =
        runTest {
            // GIVEN
            val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
            whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
            val viewModel = createViewModel()

            // WHEN
            eventsFlow.emit(ChildToParentEvent.ExitPosClicked)

            // THEN
            assertThat(viewModel.state.value.exitConfirmationDialog)
                .isEqualTo(WooPosExitConfirmationDialog)
        }

    @Test
    fun `given state is Cart NotEmpty, when ExitPosClicked passed, then exit confirmation dialog should be shown`() =
        runTest {
            // GIVEN
            val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
            whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
            val viewModel = createViewModel()

            // WHEN
            eventsFlow.emit(ChildToParentEvent.CartStatusChanged.NotEmpty)
            eventsFlow.emit(ChildToParentEvent.ExitPosClicked)

            // THEN
            assertThat(viewModel.state.value.exitConfirmationDialog)
                .isEqualTo(WooPosExitConfirmationDialog)
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
        assertTrue((viewModel.state.value.productsInfoDialog is WooPosHomeState.ProductsInfoDialog.Visible))
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
        assertThat(
            (viewModel.state.value.productsInfoDialog as WooPosHomeState.ProductsInfoDialog.Visible).header
        ).isEqualTo(
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
        assertThat(
            (viewModel.state.value.productsInfoDialog as WooPosHomeState.ProductsInfoDialog.Visible).primaryMessage
        ).isEqualTo(
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
        assertThat(
            (viewModel.state.value.productsInfoDialog as WooPosHomeState.ProductsInfoDialog.Visible).secondaryMessage
        ).isEqualTo(
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
        assertThat(
            (viewModel.state.value.productsInfoDialog as WooPosHomeState.ProductsInfoDialog.Visible).primaryButton.label
        ).isEqualTo(
            R.string.woopos_dialog_products_info_button_label
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
        assertFalse((viewModel.state.value.productsInfoDialog is WooPosHomeState.ProductsInfoDialog.Visible))
    }

    @Test
    fun `given home screen is at checkout, when products are updated, then should not modify screen position`() {
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(
                ChildToParentEvent.CheckoutClicked(listOf(1)),
                ChildToParentEvent.ProductsStatusChanged.FullScreen
            )
        )
        val viewModel = createViewModel()

        assertTrue(viewModel.state.value.screenPositionState is WooPosHomeState.ScreenPositionState.Checkout)
    }

    private fun createViewModel() = WooPosHomeViewModel(
        childrenToParentEventReceiver,
        parentToChildrenEventSender,
        SavedStateHandle()
    )
}
