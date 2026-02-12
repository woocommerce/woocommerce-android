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
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.totals.WooPosCardReaderPaymentControllerFactory
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

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
        on { create(any(), any(), any()) }.thenReturn(paymentController)
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
    private val priceFormat: WooPosFormatPrice = mock()
    private val totalsRepository: WooPosTotalsRepository = mock()
    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker = mock()

    private lateinit var viewModel: WooPosCardPaymentViewModel

    @Before
    fun setUp() = runTest {
        val mockOrder = mock<Order> {
            on { total }.thenReturn(BigDecimal("50.00"))
        }
        whenever(totalsRepository.getOrderById(any())).thenReturn(mockOrder)
        whenever(priceFormat(any<BigDecimal>())).thenReturn("$50.00")
    }

    private fun createViewModel(
        orderId: Long = 100L,
        source: CardPaymentSource = CardPaymentSource.BOOKINGS,
    ): WooPosCardPaymentViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CARD_PAYMENT_ROUTE_ORDER_ID_KEY to orderId,
                CARD_PAYMENT_ROUTE_SOURCE_KEY to source.name,
            )
        )
        return WooPosCardPaymentViewModel(
            savedState = savedStateHandle,
            cardReaderPaymentControllerFactory = cardReaderPaymentControllerFactory,
            cardReaderFacade = cardReaderFacade,
            networkStatus = networkStatus,
            resourceProvider = resourceProvider,
            uiStringParser = uiStringParser,
            priceFormat = priceFormat,
            totalsRepository = totalsRepository,
            analyticsTracker = analyticsTracker,
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
    fun `given connected reader, when controller emits CollectingPayment, then state is Collecting ReadyForPayment`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerPaymentState.value = CardReaderPaymentState.CollectingPayment
            .ExternalReaderCollectPaymentState(
                amountWithCurrencyLabel = "$50.00",
                onCancel = {}
            )
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.Collecting.ReadyForPayment::class.java)
    }

    @Test
    fun `given connected reader, when controller emits ProcessingPayment, then state is PaymentInProgress`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerPaymentState.value = CardReaderPaymentState.ProcessingPayment
            .ExternalReaderProcessingPayment(
                amountWithCurrencyLabel = "$50.00",
                onCancel = {}
            )
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.PaymentInProgress::class.java)
    }

    @Test
    fun `given connected reader, when controller emits PaymentSuccessful, then state is PaymentSuccess`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerPaymentState.value = CardReaderPaymentState.PaymentSuccessful
            .ExternalReaderPaymentSuccessful(
                amountWithCurrencyLabel = "$50.00",
                onPrintReceiptClicked = {},
                onSendReceiptClicked = {},
                onSaveUserClicked = {}
            )
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.PaymentSuccess::class.java)
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
    fun `given disconnected reader, when onConnectReaderClicked, then connectToReader called`() = runTest {
        readerStatusFlow.value = CardReaderStatus.NotConnected()

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onConnectReaderClicked()

        verify(cardReaderFacade).connectToReader()
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
    fun `given collecting state, when onBackClicked, then GoBack emitted`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onBackClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.GoBack::class.java)
        }
    }

    @Test
    fun `given BOOKINGS source, when onDoneClicked, then GoBackWithResult emitted`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.BOOKINGS)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onDoneClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.GoBackWithResult::class.java)
            val resultEvent = event as WooPosNavigationEvent.GoBackWithResult
            assertThat(resultEvent.key).isEqualTo(BOOKING_CARD_PAYMENT_SUCCESS_KEY)
            assertThat(resultEvent.value).isEqualTo(true)
        }
    }

    @Test
    fun `given CHECKOUT source, when onDoneClicked, then GoBack emitted`() = runTest {
        viewModel = createViewModel(source = CardPaymentSource.CHECKOUT)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onDoneClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.GoBack::class.java)
        }
    }

    @Test
    fun `when onEmailReceiptClicked, then OpenEmailReceipt emitted`() = runTest {
        viewModel = createViewModel(orderId = 42L)
        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onEmailReceiptClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.OpenEmailReceipt::class.java)
            assertThat((event as WooPosNavigationEvent.OpenEmailReceipt).orderId).isEqualTo(42L)
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
    fun `given controller emits ShowErrorMessage, when collecting, then state is PaymentFailed`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        controllerEventFlow.emit(
            CardReaderPaymentEvent.ShowErrorMessage(
                com.woocommerce.android.R.string.card_reader_payment_order_paid_payment_cancelled
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isInstanceOf(WooPosCardPaymentState.PaymentFailed::class.java)
    }

    @Test
    fun `given connected reader, when payment starts collecting, then trackPaymentStates called`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        verify(analyticsTracker).trackPaymentStates(any())
    }

    @Test
    fun `when onEmailReceiptClicked, then trackEmailReceiptTapped called`() = runTest {
        viewModel = createViewModel(orderId = 42L)
        advanceUntilIdle()

        viewModel.onEmailReceiptClicked()
        advanceUntilIdle()

        verify(analyticsTracker).trackEmailReceiptTapped()
    }
}
