package com.woocommerce.android.ui.woopos.paymentsuccess

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.cardpayment.WooPosCardPaymentAnalyticsTracker
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptButtonState
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptCoordinator
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptEvent
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptFailure
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosPrintReceiptFailureAction
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPaymentSuccessViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker = mock()
    private val totalsRepository: WooPosTotalsRepository = mock()
    private val priceFormat: WooPosFormatPrice = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val printReceiptEvents = MutableSharedFlow<WooPosPrintReceiptEvent>()
    private val printReceiptButtonState = MutableStateFlow(WooPosPrintReceiptButtonState.Idle)
    private val printReceiptCoordinator: WooPosPrintReceiptCoordinator = mock {
        on { events } doReturn printReceiptEvents
        on { buttonState } doReturn printReceiptButtonState
    }
    private val printReceiptCoordinatorFactory: WooPosPrintReceiptCoordinator.Factory = mock {
        on { create(any()) } doReturn printReceiptCoordinator
    }

    private fun createViewModel(
        orderId: Long = 123L,
        source: PaymentSuccessSource = PaymentSuccessSource.CARD_CHECKOUT,
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            mapOf(
                PAYMENT_SUCCESS_ORDER_ID_KEY to orderId,
                PAYMENT_SUCCESS_SOURCE_KEY to source.name,
            )
        ),
    ): WooPosPaymentSuccessViewModel {
        return WooPosPaymentSuccessViewModel(
            analyticsTracker = analyticsTracker,
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
            resourceProvider = resourceProvider,
            printReceiptCoordinatorFactory = printReceiptCoordinatorFactory,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `given card source and order exists, when init, then state has card payment text`() = runTest {
        // GIVEN
        val order = Order.getEmptyOrder(Date(), Date()).copy(total = BigDecimal("50.00"))
        whenever(totalsRepository.getOrderById(123L)).thenReturn(order)
        whenever(priceFormat(BigDecimal("50.00"))).thenReturn("$50.00")
        whenever(resourceProvider.getString(R.string.woopos_totals_success_payment_card, "$50.00"))
            .thenReturn("A card payment of $50.00 was successfully made.")

        // WHEN
        val viewModel = createViewModel(source = PaymentSuccessSource.CARD_CHECKOUT)

        // THEN
        assertThat(viewModel.state.value.orderTotalText)
            .isEqualTo("A card payment of $50.00 was successfully made.")
    }

    @Test
    fun `given null order, when init, then state has fallback text`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(123L)).thenReturn(null)
        whenever(resourceProvider.getString(R.string.woopos_payment_successful_label))
            .thenReturn("Payment successful")

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.orderTotalText).isEqualTo("Payment successful")
    }

    @Test
    fun `given order with billing email, when init, then state has receipt message`() = runTest {
        // GIVEN
        val order = Order.getEmptyOrder(Date(), Date()).copy(
            total = BigDecimal("50.00"),
            customer = Order.Customer(
                billingAddress = Address.EMPTY.copy(email = "customer@example.com"),
                shippingAddress = Address.EMPTY
            )
        )
        whenever(totalsRepository.getOrderById(123L)).thenReturn(order)
        whenever(priceFormat(any<BigDecimal>())).thenReturn("$50.00")
        whenever(resourceProvider.getString(any(), any())).thenReturn("test")
        whenever(resourceProvider.getString(R.string.woopos_receipt_sent_to_customer, "customer@example.com"))
            .thenReturn("A receipt has been sent to customer@example.com.")

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.receiptSentMessage)
            .isEqualTo("A receipt has been sent to customer@example.com.")
    }

    @Test
    fun `given order without billing email, when init, then receipt message is null`() = runTest {
        // GIVEN
        val order = Order.getEmptyOrder(Date(), Date()).copy(total = BigDecimal("50.00"))
        whenever(totalsRepository.getOrderById(123L)).thenReturn(order)
        whenever(priceFormat(any<BigDecimal>())).thenReturn("$50.00")
        whenever(resourceProvider.getString(any(), any())).thenReturn("test")

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.receiptSentMessage).isNull()
    }

    @Test
    fun `given CARD_CHECKOUT source, when onDoneClicked, then GoBack emitted`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel(source = PaymentSuccessSource.CARD_CHECKOUT)

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onDoneClicked()
            assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
        }
    }

    @Test
    fun `when onEmailReceiptClicked, then trackEmailReceiptTapped is called`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel()

        // WHEN
        viewModel.onEmailReceiptClicked()

        // THEN
        verify(analyticsTracker).trackEmailReceiptTapped()
    }

    @Test
    fun `when onEmailReceiptClicked, then the coordinator ignores the pending print failure`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel()

        // WHEN
        viewModel.onEmailReceiptClicked()

        // THEN
        verify(printReceiptCoordinator).ignorePendingFailure()
    }

    @Test
    fun `when onEmailReceiptClicked, then OpenEmailReceipt navigation event emitted`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel(orderId = 42L)

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onEmailReceiptClicked()
            val event = awaitItem()
            assertThat(event).isEqualTo(WooPosNavigationEvent.OpenEmailReceipt(42L))
        }
    }

    @Test
    fun `when the coordinator button state changes, then the print receipt button state follows it`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel()

        // WHEN
        printReceiptButtonState.value = WooPosPrintReceiptButtonState.Loading

        // THEN
        assertThat(viewModel.printReceiptButtonState.value).isEqualTo(WooPosPrintReceiptButtonState.Loading)
    }

    @Test
    fun `when print receipt is clicked, then the coordinator gets the order id`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel(orderId = 42L)

        // WHEN
        viewModel.onPrintReceiptClicked()

        // THEN
        verify(printReceiptCoordinator).onPrintReceiptClicked(42L)
    }

    @Test
    fun `when the coordinator asks for printer setup, then the setup dialog is shown`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel()

        // WHEN
        printReceiptEvents.emit(WooPosPrintReceiptEvent.ShowPrinterSetup)

        // THEN
        assertThat(viewModel.state.value.isReceiptPrinterSetupDialogVisible).isTrue()
    }

    @Test
    fun `given the setup dialog is shown, when it is dismissed, then it is hidden and nothing is printed`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(any())).thenReturn("test")
            val viewModel = createViewModel()
            printReceiptEvents.emit(WooPosPrintReceiptEvent.ShowPrinterSetup)

            // WHEN
            viewModel.onReceiptPrinterSetupDialogDismissed()

            // THEN
            assertThat(viewModel.state.value.isReceiptPrinterSetupDialogVisible).isFalse()
            verify(printReceiptCoordinator, never()).onPrinterConnected(any())
        }

    @Test
    fun `given the setup dialog is shown, when a printer connects, then it is hidden and the coordinator gets the order id`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(any())).thenReturn("test")
            val viewModel = createViewModel(orderId = 42L)
            printReceiptEvents.emit(WooPosPrintReceiptEvent.ShowPrinterSetup)

            // WHEN
            viewModel.onReceiptPrinterConnected()

            // THEN
            assertThat(viewModel.state.value.isReceiptPrinterSetupDialogVisible).isFalse()
            verify(printReceiptCoordinator).onPrinterConnected(42L)
        }

    @Test
    fun `given the setup dialog was shown, when the ViewModel is recreated from saved state, then the dialog is shown`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(any())).thenReturn("test")
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    PAYMENT_SUCCESS_ORDER_ID_KEY to 42L,
                    PAYMENT_SUCCESS_SOURCE_KEY to PaymentSuccessSource.CARD_CHECKOUT.name,
                )
            )
            createViewModel(savedStateHandle = savedStateHandle)
            printReceiptEvents.emit(WooPosPrintReceiptEvent.ShowPrinterSetup)

            // WHEN
            val recreatedViewModel = createViewModel(savedStateHandle = savedStateHandle)

            // THEN
            assertThat(recreatedViewModel.state.value.isReceiptPrinterSetupDialogVisible).isTrue()
        }

    @Test
    fun `when the coordinator reports a failed print, then the failure is shown`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel()

        // WHEN
        printReceiptEvents.emit(WooPosPrintReceiptEvent.PrintFailed(42L, WooPosPrintReceiptFailure.CoverOpen))

        // THEN
        assertThat(viewModel.state.value.printReceiptFailure).isEqualTo(WooPosPrintReceiptFailure.CoverOpen)
    }

    @Test
    fun `given a print failure was shown, when the ViewModel is recreated from saved state, then the failure is shown`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(any())).thenReturn("test")
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    PAYMENT_SUCCESS_ORDER_ID_KEY to 42L,
                    PAYMENT_SUCCESS_SOURCE_KEY to PaymentSuccessSource.CARD_CHECKOUT.name,
                )
            )
            createViewModel(savedStateHandle = savedStateHandle)
            printReceiptEvents.emit(WooPosPrintReceiptEvent.PrintFailed(42L, WooPosPrintReceiptFailure.CoverOpen))

            // WHEN
            val recreatedViewModel = createViewModel(savedStateHandle = savedStateHandle)

            // THEN
            assertThat(recreatedViewModel.state.value.printReceiptFailure)
                .isEqualTo(WooPosPrintReceiptFailure.CoverOpen)
        }

    @Test
    fun `given a restored print failure, when it is dismissed, then it is not restored again`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val savedStateHandle = SavedStateHandle(
            mapOf(
                PAYMENT_SUCCESS_ORDER_ID_KEY to 42L,
                PAYMENT_SUCCESS_SOURCE_KEY to PaymentSuccessSource.CARD_CHECKOUT.name,
            )
        )
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)
        printReceiptEvents.emit(WooPosPrintReceiptEvent.PrintFailed(42L, WooPosPrintReceiptFailure.CoverOpen))

        // WHEN
        viewModel.onPrintReceiptFailureDialogDismissed()

        // THEN
        assertThat(createViewModel(savedStateHandle = savedStateHandle).state.value.printReceiptFailure).isNull()
    }

    @Test
    fun `given the view model was cleared, when the coordinator reports a failed print, then the failure is not shown`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(any())).thenReturn("test")
            val viewModel = createViewModel()
            viewModel.viewModelScope.cancel()

            // WHEN
            printReceiptEvents.emit(WooPosPrintReceiptEvent.PrintFailed(42L, WooPosPrintReceiptFailure.CoverOpen))

            // THEN
            assertThat(viewModel.state.value.printReceiptFailure).isNull()
        }

    @Test
    fun `given a print failure is shown, when it is dismissed, then it is hidden and nothing is printed`() = runTest {
        // GIVEN
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel()
        printReceiptEvents.emit(WooPosPrintReceiptEvent.PrintFailed(42L, WooPosPrintReceiptFailure.CoverOpen))

        // WHEN
        viewModel.onPrintReceiptFailureDialogDismissed()

        // THEN
        assertThat(viewModel.state.value.printReceiptFailure).isNull()
        verify(printReceiptCoordinator, never()).onPrintReceiptClicked(any())
    }

    @Test
    fun `given a print failure is shown, when retry is selected, then it is hidden and the coordinator gets the order id`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(any())).thenReturn("test")
            val viewModel = createViewModel(orderId = 42L)
            printReceiptEvents.emit(WooPosPrintReceiptEvent.PrintFailed(42L, WooPosPrintReceiptFailure.CoverOpen))

            // WHEN
            viewModel.onPrintReceiptFailureActionSelected(WooPosPrintReceiptFailureAction.Retry)

            // THEN
            assertThat(viewModel.state.value.printReceiptFailure).isNull()
            verify(printReceiptCoordinator).onPrintReceiptClicked(42L)
        }

    @Test
    fun `given a print failure is shown, when email receipt is selected, then it is hidden and email receipt opens`() =
        runTest {
            // GIVEN
            whenever(resourceProvider.getString(any())).thenReturn("test")
            val viewModel = createViewModel(orderId = 42L)
            printReceiptEvents.emit(WooPosPrintReceiptEvent.PrintFailed(42L, WooPosPrintReceiptFailure.CoverOpen))

            viewModel.navigationEvent.test {
                // WHEN
                viewModel.onPrintReceiptFailureActionSelected(WooPosPrintReceiptFailureAction.EmailReceipt)

                // THEN
                assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.OpenEmailReceipt(42L))
                assertThat(viewModel.state.value.printReceiptFailure).isNull()
            }
        }
}
