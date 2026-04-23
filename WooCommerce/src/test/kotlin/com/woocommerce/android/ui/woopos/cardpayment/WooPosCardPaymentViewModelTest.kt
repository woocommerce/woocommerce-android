package com.woocommerce.android.ui.woopos.cardpayment

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.bookings.BOOKING_PAYMENT_FLOW_FINISHED_KEY
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderPaymentFlow
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderSession
import com.woocommerce.android.ui.woopos.cashpayment.CashPaymentSource
import com.woocommerce.android.ui.woopos.home.totals.WooPosCardReaderPaymentControllerFactory
import com.woocommerce.android.ui.woopos.paymentsuccess.PaymentSuccessSource
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCardPaymentViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val readerStatusFlow = MutableStateFlow<CardReaderStatus>(CardReaderStatus.Connected(mock<CardReader>()))
    private val cardReaderFacade: WooPosCardReaderFacade = mock {
        on { readerStatus }.thenReturn(readerStatusFlow)
    }
    private val controllerPaymentState =
        MutableStateFlow<CardReaderPaymentState>(CardReaderPaymentState.LoadingData { })
    private val controllerEventFlow = MutableSharedFlow<CardReaderPaymentEvent>()
    private val paymentController: CardReaderPaymentController = mock {
        on { paymentState }.thenReturn(controllerPaymentState)
        on { event }.thenReturn(controllerEventFlow)
    }
    private val cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory = mock {
        on { create(any(), any(), any(), any()) }.thenReturn(paymentController)
    }
    private val networkStatus: WooPosNetworkStatus = mock {
        on { isConnected() }.thenReturn(true)
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) }.thenReturn("test string")
        on { getString(any(), any()) }.thenReturn("test string with arg")
    }
    private val uiStringParser: UiStringParser = mock {
        on { asString(any()) }.thenReturn("parsed error")
    }
    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker = mock()
    private val cardPaymentRepository: WooPosCardPaymentRepository = mock()
    private val priceFormat: WooPosFormatPrice = mock {
        on { invoke(any<BigDecimal>()) } doReturn "$0.00"
    }
    private val remoteReaderSessionStateFlow =
        MutableStateFlow<WooPosRemoteReaderSession.State>(WooPosRemoteReaderSession.State.Idle)
    private val remoteReaderSession: WooPosRemoteReaderSession = mock {
        on { state }.thenReturn(remoteReaderSessionStateFlow)
    }
    private val remoteReaderPaymentFlow: WooPosRemoteReaderPaymentFlow = mock()

    private val testOrder: Order = Order.getEmptyOrder(Date(), Date()).copy(
        productsTotal = BigDecimal.TEN,
        discountTotal = BigDecimal.ZERO,
        totalTax = BigDecimal.ONE,
        total = BigDecimal.TEN,
    )

    private lateinit var viewModel: WooPosCardPaymentViewModel

    @Before
    fun setUp() {
        runBlocking {
            whenever(cardPaymentRepository.fetchOrGetOrder(any())).thenReturn(testOrder)
        }
    }

    private fun createViewModel(
        orderId: Long = 100L,
        source: CardPaymentSource = CardPaymentSource.BOOKINGS,
        showCashPaymentButton: Boolean = false,
    ): WooPosCardPaymentViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CARD_PAYMENT_ROUTE_ORDER_ID_KEY to orderId,
                CARD_PAYMENT_ROUTE_SOURCE_KEY to source.name,
                CARD_PAYMENT_ROUTE_SHOW_CASH_PAYMENT_KEY to showCashPaymentButton,
            )
        )
        return WooPosCardPaymentViewModel(
            savedState = savedStateHandle,
            cardReaderPaymentControllerFactory = cardReaderPaymentControllerFactory,
            cardReaderFacade = cardReaderFacade,
            networkStatus = networkStatus,
            resourceProvider = resourceProvider,
            uiStringParser = uiStringParser,
            analyticsTracker = analyticsTracker,
            cardPaymentRepository = cardPaymentRepository,
            priceFormat = priceFormat,
            remoteReaderSession = remoteReaderSession,
            remoteReaderPaymentFlow = remoteReaderPaymentFlow,
        )
    }

    @Test
    fun `given connected reader, when init, then state is Collecting Preparing`() = runTest {
        controllerPaymentState.value = CardReaderPaymentState.LoadingData { }

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value).isInstanceOf(WooPosCardPaymentState.Collecting.Preparing::class.java)
    }

    @Test
    fun `given connected reader, when controller emits ProcessingPayment, then state is Collecting ReadyForPayment`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerPaymentState.value = CardReaderPaymentState.ProcessingPayment
            .ExternalReaderProcessingPayment(
                amountWithCurrencyLabel = "$50.00",
                onCancel = {}
            )
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.Collecting.ReadyForPayment::class.java)
    }

    @Test
    fun `given connected reader, when controller emits PaymentCapturing, then state is PaymentInProgress`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerPaymentState.value = CardReaderPaymentState.PaymentCapturing
            .ExternalReaderPaymentCapturing(
                amountWithCurrencyLabel = "$50.00",
            )
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.PaymentInProgress::class.java)
    }

    @Test
    fun `given connected reader, when controller emits PaymentSuccessful, then OpenPaymentSuccess nav event emitted`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.BOOKINGS)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            controllerPaymentState.value = CardReaderPaymentState.PaymentSuccessful
                .ExternalReaderPaymentSuccessful(
                    amountWithCurrencyLabel = "$50.00",
                    onPrintReceiptClicked = {},
                    onSendReceiptClicked = {},
                    onSaveUserClicked = {}
                )
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.OpenPaymentSuccess::class.java)
            val successEvent = event as WooPosNavigationEvent.OpenPaymentSuccess
            assertThat(successEvent.source).isEqualTo(PaymentSuccessSource.CARD_BOOKINGS)
        }
    }

    @Test
    fun `given CHECKOUT source, when controller emits PaymentSuccessful, then OpenPaymentSuccess with CARD_CHECKOUT`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.CHECKOUT)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            controllerPaymentState.value = CardReaderPaymentState.PaymentSuccessful
                .ExternalReaderPaymentSuccessful(
                    amountWithCurrencyLabel = "$50.00",
                    onPrintReceiptClicked = {},
                    onSendReceiptClicked = {},
                    onSaveUserClicked = {}
                )
            advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.OpenPaymentSuccess::class.java)
            val successEvent = event as WooPosNavigationEvent.OpenPaymentSuccess
            assertThat(successEvent.source).isEqualTo(PaymentSuccessSource.CARD_CHECKOUT)
        }
    }

    @Test
    fun `given connected reader, when controller emits PaymentFailed with retry, then state is PaymentFailed with isDismissButtonVisible true`() =
        runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            controllerPaymentState.value = CardReaderPaymentState.PaymentFailed
                .ExternalReaderFailedPayment.Cancelable(
                    errorType = PaymentFlowError.Generic,
                    amountWithCurrencyLabel = "$50.00",
                    onCancel = {},
                    onRetry = {},
                )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosCardPaymentState.PaymentFailed::class.java)
            assertThat((state as WooPosCardPaymentState.PaymentFailed).isDismissButtonVisible).isTrue()
        }

    @Test
    fun `given connected reader, when controller emits PaymentFailed without retry, then state is PaymentFailed`() =
        runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            controllerPaymentState.value = CardReaderPaymentState.PaymentFailed
                .ExternalReaderFailedPayment.NonCancelable(
                    errorType = PaymentFlowError.Generic,
                    onRetry = {},
                )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosCardPaymentState.PaymentFailed::class.java)
        }

    @Test
    fun `given disconnected reader, when init, then state is Collecting ReaderDisconnected`() = runTest {
        readerStatusFlow.value = CardReaderStatus.NotConnected()

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.Collecting.ReaderDisconnected::class.java)
    }

    @Test
    fun `given disconnected reader, when onConnectReaderClicked, then GoBack emitted`() = runTest {
        readerStatusFlow.value = CardReaderStatus.NotConnected()

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onConnectReaderClicked()
            assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
        }
    }

    @Test
    fun `given processing state, when onBackClicked, then no GoBack emitted`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerPaymentState.value = CardReaderPaymentState.ProcessingPayment
            .ExternalReaderProcessingPayment(
                amountWithCurrencyLabel = "$50.00",
                onCancel = {}
            )
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onBackClicked()
            expectNoEvents()
        }
    }

    @Test
    fun `given CHECKOUT source and collecting state, when onBackClicked, then GoBack emitted`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.CHECKOUT)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onBackClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.GoBack::class.java)
        }
    }

    @Test
    fun `given BOOKINGS source, when onBackClicked, then NavigateBackToBookingsAfterPayment emitted`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.BOOKINGS)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onBackClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.NavigateBackToBookingsAfterPayment::class.java)
            val navEvent = event as WooPosNavigationEvent.NavigateBackToBookingsAfterPayment
            assertThat(navEvent.key).isEqualTo(BOOKING_PAYMENT_FLOW_FINISHED_KEY)
            assertThat(navEvent.value).isEqualTo(true)
        }
    }

    @Test
    fun `given no network, when init, then state is PaymentFailed`() = runTest {
        whenever(networkStatus.isConnected()).thenReturn(false)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.PaymentFailed::class.java)
    }

    @Test
    fun `given controller emits ShowErrorMessage, when collecting, then state is PaymentFailed with no action button`() =
        runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            controllerEventFlow.emit(
                CardReaderPaymentEvent.ShowErrorMessage(
                    com.woocommerce.android.R.string.card_reader_payment_order_paid_payment_cancelled
                )
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosCardPaymentState.PaymentFailed::class.java)
            val failedState = state as WooPosCardPaymentState.PaymentFailed
            assertThat(failedState.actionButtonLabel).isNull()
            assertThat(failedState.isDismissButtonVisible).isTrue()
        }

    @Test
    fun `given controller emits ShowPaymentErrorMessage, when collecting, then state is PaymentFailed with no action button`() =
        runTest {
            viewModel = createViewModel()
            advanceUntilIdle()

            controllerEventFlow.emit(
                CardReaderPaymentEvent.ShowPaymentErrorMessage(
                    com.woocommerce.android.R.string.card_reader_payment_order_paid_payment_cancelled
                )
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosCardPaymentState.PaymentFailed::class.java)
            val failedState = state as WooPosCardPaymentState.PaymentFailed
            assertThat(failedState.actionButtonLabel).isNull()
            assertThat(failedState.isDismissButtonVisible).isTrue()
        }

    @Test
    fun `given connected reader, when payment starts collecting, then trackPaymentStates called`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        verify(analyticsTracker).trackPaymentStates(any())
    }

    @Test
    fun `given showCashPaymentButton is true, when created, then showCashPaymentButton is true`() = runTest {
        viewModel = createViewModel(showCashPaymentButton = true)
        advanceUntilIdle()

        assertThat(viewModel.showCashPaymentButton).isTrue()
    }

    @Test
    fun `given showCashPaymentButton is false, when created, then showCashPaymentButton is false`() = runTest {
        viewModel = createViewModel(showCashPaymentButton = false)
        advanceUntilIdle()

        assertThat(viewModel.showCashPaymentButton).isFalse()
    }

    @Test
    fun `given showCashPaymentButton not provided, when created, then showCashPaymentButton is false`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CARD_PAYMENT_ROUTE_ORDER_ID_KEY to 100L,
                CARD_PAYMENT_ROUTE_SOURCE_KEY to CardPaymentSource.CHECKOUT.name,
            )
        )
        viewModel = WooPosCardPaymentViewModel(
            savedState = savedStateHandle,
            cardReaderPaymentControllerFactory = cardReaderPaymentControllerFactory,
            cardReaderFacade = cardReaderFacade,
            networkStatus = networkStatus,
            resourceProvider = resourceProvider,
            uiStringParser = uiStringParser,
            analyticsTracker = analyticsTracker,
            cardPaymentRepository = cardPaymentRepository,
            priceFormat = priceFormat,
            remoteReaderSession = remoteReaderSession,
            remoteReaderPaymentFlow = remoteReaderPaymentFlow,
        )
        advanceUntilIdle()

        assertThat(viewModel.showCashPaymentButton).isFalse()
    }

    @Test
    fun `given BOOKINGS source, when onCashPaymentClicked, then NavigateToCashPayment emitted with BOOKINGS source`() = runTest {
        viewModel = createViewModel(orderId = 42L, source = CardPaymentSource.BOOKINGS)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onCashPaymentClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.NavigateToCashPayment::class.java)
            val cashEvent = event as WooPosNavigationEvent.NavigateToCashPayment
            assertThat(cashEvent.orderId).isEqualTo(42L)
            assertThat(cashEvent.source).isEqualTo(CashPaymentSource.BOOKINGS)
        }
    }

    @Test
    fun `given CHECKOUT source, when onCashPaymentClicked, then NavigateToCashPayment emitted with CHECKOUT source`() = runTest {
        viewModel = createViewModel(orderId = 42L, source = CardPaymentSource.CHECKOUT)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onCashPaymentClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.NavigateToCashPayment::class.java)
            val cashEvent = event as WooPosNavigationEvent.NavigateToCashPayment
            assertThat(cashEvent.orderId).isEqualTo(42L)
            assertThat(cashEvent.source).isEqualTo(CashPaymentSource.CHECKOUT)
        }
    }

    @Test
    fun `given BOOKINGS source and order already paid error, when onBackClicked, then NavigateBackToBookingsAfterPayment emitted`() =
        runTest {
            viewModel = createViewModel(source = CardPaymentSource.BOOKINGS)
            advanceUntilIdle()

            controllerEventFlow.emit(
                CardReaderPaymentEvent.ShowErrorMessage(
                    com.woocommerce.android.R.string.card_reader_payment_order_paid_payment_cancelled
                )
            )
            advanceUntilIdle()

            viewModel.navigationEvent.test {
                viewModel.onBackClicked()

                val event = awaitItem()
                assertThat(event).isInstanceOf(WooPosNavigationEvent.NavigateBackToBookingsAfterPayment::class.java)
                val navEvent = event as WooPosNavigationEvent.NavigateBackToBookingsAfterPayment
                assertThat(navEvent.key).isEqualTo(BOOKING_PAYMENT_FLOW_FINISHED_KEY)
                assertThat(navEvent.value).isEqualTo(true)
            }
        }

    @Test
    fun `given BOOKINGS source and order already paid error, when onDismissClicked, then NavigateBackToBookingsAfterPayment emitted`() =
        runTest {
            viewModel = createViewModel(source = CardPaymentSource.BOOKINGS)
            advanceUntilIdle()

            controllerEventFlow.emit(
                CardReaderPaymentEvent.ShowErrorMessage(
                    com.woocommerce.android.R.string.card_reader_payment_order_paid_payment_cancelled
                )
            )
            advanceUntilIdle()

            viewModel.navigationEvent.test {
                viewModel.onDismissClicked()

                val event = awaitItem()
                assertThat(event).isInstanceOf(WooPosNavigationEvent.NavigateBackToBookingsAfterPayment::class.java)
                val navEvent = event as WooPosNavigationEvent.NavigateBackToBookingsAfterPayment
                assertThat(navEvent.key).isEqualTo(BOOKING_PAYMENT_FLOW_FINISHED_KEY)
                assertThat(navEvent.value).isEqualTo(true)
            }
        }

    @Test
    fun `given CHECKOUT source and order already paid error, when onBackClicked, then GoBack emitted`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.CHECKOUT)
        advanceUntilIdle()

        controllerEventFlow.emit(
            CardReaderPaymentEvent.ShowErrorMessage(
                com.woocommerce.android.R.string.card_reader_payment_order_paid_payment_cancelled
            )
        )
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onBackClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.GoBack::class.java)
        }
    }

    @Test
    fun `given BOOKINGS source, when onCashPaymentClicked, then payment is cancelled`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.BOOKINGS)
        advanceUntilIdle()

        viewModel.onCashPaymentClicked()
        advanceUntilIdle()

        verify(paymentController).onBackPressed()
        verify(paymentController).stop()
    }

    @Test
    fun `given payment cancelled by cash navigation, when screen resumed, then payment restarts`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.BOOKINGS)
        advanceUntilIdle()

        viewModel.onCashPaymentClicked()
        advanceUntilIdle()

        viewModel.onScreenResumed()
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.Collecting.Preparing::class.java)
    }

    @Test
    fun `given reader disconnected after cash navigation, when screen resumed, then payment does not restart`() =
        runTest {
            viewModel = createViewModel(source = CardPaymentSource.BOOKINGS)
            advanceUntilIdle()

            viewModel.onCashPaymentClicked()
            advanceUntilIdle()

            readerStatusFlow.value = CardReaderStatus.NotConnected()
            advanceUntilIdle()

            viewModel.onScreenResumed()
            advanceUntilIdle()

            assertThat(viewModel.state.value)
                .isInstanceOf(WooPosCardPaymentState.Collecting.ReaderDisconnected::class.java)
        }

    @Test
    fun `given payment failed, when screen resumed, then payment is not restarted`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerPaymentState.value = CardReaderPaymentState.PaymentFailed
            .ExternalReaderFailedPayment.Cancelable(
                errorType = PaymentFlowError.Generic,
                amountWithCurrencyLabel = "$50.00",
                onCancel = {},
                onRetry = {},
            )
        advanceUntilIdle()

        viewModel.onScreenResumed()
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.PaymentFailed::class.java)
    }

    @Test
    fun `given collecting state, when screen paused, then payment is cancelled`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onScreenPaused()
        advanceUntilIdle()

        verify(paymentController).onBackPressed()
        verify(paymentController).stop()
    }

    @Test
    fun `given payment in progress, when screen paused, then payment is not cancelled`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerPaymentState.value = CardReaderPaymentState.PaymentCapturing
            .ExternalReaderPaymentCapturing(
                amountWithCurrencyLabel = "$50.00",
            )
        advanceUntilIdle()

        viewModel.onScreenPaused()
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.PaymentInProgress::class.java)
    }

    @Test
    fun `given order without discount, when init, then orderTotals discount is null`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value as WooPosCardPaymentState.Collecting
        assertThat(state.orderTotals.discount).isNull()
    }

    @Test
    fun `given order with discount, when init, then orderTotals discount has negative prefix`() = runTest {
        val orderWithDiscount = testOrder.copy(discountTotal = BigDecimal(5))
        whenever(cardPaymentRepository.fetchOrGetOrder(any())).thenReturn(orderWithDiscount)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value as WooPosCardPaymentState.Collecting
        assertThat(state.orderTotals.discount).startsWith("-")
    }

    @Test
    fun `given order load fails, when init, then state is PaymentFailed`() = runTest {
        whenever(cardPaymentRepository.fetchOrGetOrder(any())).thenReturn(null)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.PaymentFailed::class.java)
        val failedState = viewModel.state.value as WooPosCardPaymentState.PaymentFailed
        assertThat(failedState.isDismissButtonVisible).isTrue()
    }
}
