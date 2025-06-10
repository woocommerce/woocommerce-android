package com.woocommerce.android.ui.woopos.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.util.WooPosSoundHelper
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent.ExitPosClicked
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent.SystemBackClicked
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCartTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ExitConfirmed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

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
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val soundHelper: WooPosSoundHelper = mock()

    @Test
    fun `when order created, then pass event to cart`() =
        runTest {
            // GIVEN
            whenever(childrenToParentEventReceiver.events).thenReturn(
                flowOf(ChildToParentEvent.OrderCreated(emptyList(), emptyList()))
            )

            // WHEN
            createViewModel()

            // THEN
            verify(parentToChildrenEventSender).sendToChildren(
                argThat {
                    this is ParentToChildrenEvent.OrderCreated
                }
            )
        }

    @Test
    fun `given screen state Cart, when barcode scanned, then pass event to children`() = runTest {
        // GIVEN
        val events = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(events)
        createViewModel()

        // WHEN
        events.emit(ChildToParentEvent.BarcodeScanned("123456789"))

        // THEN
        verify(parentToChildrenEventSender).sendToChildren(
            argThat {
                this is ParentToChildrenEvent.BarcodeScanned &&
                    barcode == "123456789"
            }
        )
    }

    @Test
    fun `given payment completed state, when barcode scanned, then don't pass event to children`() = runTest {
        // GIVEN
        val events = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(events)

        val viewModel = createViewModel()
        events.emit(ChildToParentEvent.CheckoutClicked(listOf(ItemClickedData.Product.Simple(1))))

        assertThat(viewModel.state.value.screenPositionState)
            .isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals)

        // WHEN
        events.emit(ChildToParentEvent.BarcodeScanned("123456789"))

        // THEN
        verify(parentToChildrenEventSender, never()).sendToChildren(
            argThat {
                this is ParentToChildrenEvent.BarcodeScanned &&
                    barcode == "123456789"
            }
        )
    }

    @Test
    fun `given checkout state, when barcode scanned, then don't pass event to children`() =
        runTest {
            // GIVEN
            val events = MutableSharedFlow<ChildToParentEvent>()
            whenever(childrenToParentEventReceiver.events).thenReturn(events)
            createViewModel().onUIEvent(WooPosHomeUIEvent.OnPaymentCompletedViaCash)

            // WHEN
            events.emit(ChildToParentEvent.BarcodeScanned("123456789"))

            // THEN
            verify(parentToChildrenEventSender, never()).sendToChildren(
                argThat {
                    this is ParentToChildrenEvent.BarcodeScanned &&
                        barcode == "123456789"
                }
            )
        }

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
            assertThat(viewModel.state.value.screenPositionState)
                .isEqualTo(WooPosHomeState.ScreenPositionState.Cart)
        }

    @Test
    fun `given state is Cart, when SystemBackClicked passed, then should show exit confirmation dialog`() = runTest {
        // GIVEN
        val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(SystemBackClicked)

        // THEN
        assertThat(viewModel.state.value.exitConfirmationDialog).isEqualTo(
            WooPosHomeState.ExitConfirmationDialog(
                isVisible = true
            )
        )
    }

    @Test
    fun `given state checkout is paid, when SystemBackClicked passed, then OrderSuccessfullyPaid event should be sent`() =
        runTest {
            // GIVEN
            whenever(childrenToParentEventReceiver.events).thenReturn(
                flowOf(ChildToParentEvent.OrderSuccessfullyPaidByCard)
            )
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(SystemBackClicked)

            // THEN
            verify(parentToChildrenEventSender).sendToChildren(
                ParentToChildrenEvent.OrderSuccessfullyPaid(
                    PaymentMethod.CARD
                )
            )
            assertThat(viewModel.state.value.screenPositionState)
                .isEqualTo(WooPosHomeState.ScreenPositionState.Cart)
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
            assertThat(viewModel.state.value.exitConfirmationDialog.isVisible).isFalse()
        }

    @Test
    fun `when ExitPos confirmed in exit confirmation dialog, then should track event`() =
        runTest {
            // GIVEN
            val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
            whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(ExitPosClicked)

            // THEN
            verify(analyticsTracker).track(ExitConfirmed)
        }

    @Test
    fun `given state is Cart NotEmpty, when ExitPosClicked passed, then exit confirmation dialog should be shown`() =
        runTest {
            // GIVEN
            val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
            whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
            val viewModel = createViewModel()

            // WHEN
            eventsFlow.emit(ChildToParentEvent.ExitPosClicked)

            // THEN
            assertThat(viewModel.state.value.exitConfirmationDialog)
                .isEqualTo(
                    WooPosHomeState.ExitConfirmationDialog(
                        isVisible = true
                    )
                )
        }

    @Test
    fun `when where are my products clicked, then display products info dialog`() {
        // WHEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SimpleProductExplanationMenuItemClicked)
        )
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.productsInfoDialog).isEqualTo(
            WooPosHomeState.ProductsInfoDialog(isVisible = true)
        )
    }

    @Test
    fun `given product info dialog is displayed, then ensure dialog heading is correct`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SimpleProductExplanationMenuItemClicked)
        )
        val viewModel = createViewModel()

        // THEN
        assertThat(
            (viewModel.state.value.productsInfoDialog).header
        ).isEqualTo(
            R.string.woopos_dialog_products_info_heading
        )
    }

    @Test
    fun `given product info dialog is displayed, then ensure dialog primary message is correct`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SimpleProductExplanationMenuItemClicked)
        )
        val viewModel = createViewModel()

        // THEN
        assertThat(
            (viewModel.state.value.productsInfoDialog).primaryMessage
        ).isEqualTo(
            R.string.woopos_dialog_products_info_primary_message
        )
    }

    @Test
    fun `when where are my products clicked, then ensure dialog secondary message is correct`() {
        // WHEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SimpleProductExplanationMenuItemClicked)
        )
        val viewModel = createViewModel()

        // THEN
        assertThat(
            (viewModel.state.value.productsInfoDialog).secondaryMessage
        ).isEqualTo(
            R.string.woopos_dialog_products_info_secondary_message
        )
    }

    @Test
    fun `when where are my products clicked, then ensure dialog tertiary message is correct`() {
        // WHEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SimpleProductExplanationMenuItemClicked)
        )
        val viewModel = createViewModel()

        // THEN
        assertThat(
            (viewModel.state.value.productsInfoDialog).tertiaryMessage
        ).isEqualTo(
            R.string.woopos_dialog_products_info_tertiary_message
        )
    }

    @Test
    fun `when where are my products clicked, then ensure dialog primary button label is correct`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SimpleProductExplanationMenuItemClicked)
        )

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(
            (viewModel.state.value.productsInfoDialog).primaryButton.label
        ).isEqualTo(
            R.string.woopos_dialog_products_info_button_label
        )
    }

    @Test
    fun `given product info is displayed, when dialog is dismissed, then ensure the state is updated`() {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SimpleProductExplanationMenuItemClicked)
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosHomeUIEvent.DismissProductsInfoDialog)

        // THEN
        assertThat(viewModel.state.value.productsInfoDialog.isVisible).isFalse()
    }

    @Test
    fun `given home screen is at checkout, when go back to checkout clicked after failed payment, then should show cart with totals`() = runTest {
        // GIVEN
        val events = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(events)

        val viewModel: WooPosHomeViewModel = createViewModel()
        events.emit(ChildToParentEvent.CheckoutClicked(listOf(ItemClickedData.Product.Simple(1))))
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals)

        // WHEN
        events.emit(ChildToParentEvent.GoBackToCheckoutAfterFailedPayment)

        // THEN
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals)
    }

    @Test
    fun `given home screen is at checkout, when payment processing started, then should show full screen totals state`() = runTest {
        // GIVEN
        val events = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(events)

        val viewModel: WooPosHomeViewModel = createViewModel()
        events.emit(ChildToParentEvent.CheckoutClicked(listOf(ItemClickedData.Product.Simple(1))))
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals)

        // WHEN
        events.emit(ChildToParentEvent.PaymentInProgress)

        // THEN
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals)
    }

    @Test
    fun `given home screen is at checkout, processing payment, when payment fails, then should show full screen totals state`() = runTest {
        // GIVEN
        val events = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(events)

        val viewModel: WooPosHomeViewModel = createViewModel()
        events.emit(ChildToParentEvent.CheckoutClicked(listOf(ItemClickedData.Product.Simple(1))))
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals)
        events.emit(ChildToParentEvent.PaymentInProgress)
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals)

        // WHEN
        events.emit(ChildToParentEvent.PaymentFailed)

        // THEN
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals)
    }

    @Test
    fun `given home screen is at checkout, failed payment, when retry payment clicked, then should show cart with totals`() = runTest {
        // GIVEN
        val events = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(events)

        val viewModel: WooPosHomeViewModel = createViewModel()
        events.emit(ChildToParentEvent.CheckoutClicked(listOf(ItemClickedData.Product.Simple(1))))
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals)
        events.emit(ChildToParentEvent.PaymentInProgress)
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals)
        events.emit(ChildToParentEvent.PaymentFailed)
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals)

        // WHEN
        events.emit(ChildToParentEvent.ReturnedFromCardReaderPaymentToCheckout)

        // THEN
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals)
    }

    @Test
    fun `given state is Checkout, when OnPaymentCompletedViaCash event passed, then OrderSuccessfullyPaid event with CASH`() = runTest {
        // GIVEN
        val events = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(events)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosHomeUIEvent.OnPaymentCompletedViaCash)

        // THEN
        verify(parentToChildrenEventSender).sendToChildren(
            ParentToChildrenEvent.OrderSuccessfullyPaid(PaymentMethod.CASH)
        )
        assertThat(viewModel.state.value.screenPositionState)
            .isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.FullScreenTotals)
    }

    @Test
    fun `given OrderSuccessfullyPaid by cash, then play sound`() =
        runTest {
            // GIVEN
            val events = MutableSharedFlow<ChildToParentEvent>()
            whenever(childrenToParentEventReceiver.events).thenReturn(events)
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(WooPosHomeUIEvent.OnPaymentCompletedViaCash)

            // THEN
            verify(soundHelper).playChaChing()
        }

    @Test
    fun `given home at Checkout, when back pressed, should return to Cart`() = runTest {
        // GIVEN
        val events = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(events)

        val viewModel: WooPosHomeViewModel = createViewModel()
        events.emit(ChildToParentEvent.CheckoutClicked(listOf(ItemClickedData.Product.Simple(1))))
        assertThat(
            viewModel.state.value.screenPositionState
        ).isEqualTo(WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals)

        // WHEN
        viewModel.onUIEvent(SystemBackClicked)

        // THEN
        analyticsTracker.track(BackToCartTapped)
    }

    private fun createViewModel() = WooPosHomeViewModel(
        childrenToParentEventReceiver,
        parentToChildrenEventSender,
        analyticsTracker,
        soundHelper,
        SavedStateHandle()
    )
}
