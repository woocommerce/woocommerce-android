package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.RefreshProductsSignal
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class RefundSummaryViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
    }

    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val gatewayStore: WCGatewayStore = mock()
    private val refundStore: WCRefundStore = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { i ->
            "You can refund " + i.arguments[1].toString()
        }
    }
    private val paymentChargeRepository: PaymentChargeRepository = mock()
    private val refreshProductsSignal: RefreshProductsSignal = mock()

    private lateinit var viewModel: RefundSummaryViewModel

    private fun initViewModel(refundItems: Array<RefundItem> = emptyArray()) {
        whenever(selectedSite.get()).thenReturn(SiteModel())
        whenever(currencyFormatter.buildBigDecimalFormatter(any())).thenReturn { "" }

        viewModel = RefundSummaryViewModel(
            savedStateHandle = RefundSummaryFragmentArgs(
                orderId = ORDER_ID,
                refundAmount = "10",
                refundItems = refundItems
            ).toSavedStateHandle(),
            selectedSite = selectedSite,
            orderDetailRepository = orderDetailRepository,
            paymentChargeRepository = paymentChargeRepository,
            resourceProvider = resourceProvider,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            networkStatus = networkStatus,
            currencyFormatter = currencyFormatter,
            gatewayStore = gatewayStore,
            refundStore = refundStore,
            coroutineDispatchers = coroutinesTestRule.testDispatchers,
            refreshProductsSignal = refreshProductsSignal
        )
    }

    @Test
    fun `given non cash order, when successfully charge data loaded, then card info is visible`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "stripe",
                chargeId = chargeId
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card (Visa **** 1234)")

            initViewModel()

            val viewState = viewModel.refundSummaryStateLiveData.liveData.getOrAwaitValue()

            assertThat(viewState.refundMethod).isEqualTo("Credit/Debit card (Visa **** 1234)")
        }
    }

    @Test
    fun `given eftpos card brand, when charge data loaded, then refund method uses lower case eftpos`() {
        testBlocking {
            val chargeId = "charge_id"
            val order = createTestOrder(
                paymentMethod = "woocommerce_payments",
                chargeId = chargeId
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(order)
            whenever(gatewayStore.getGateway(any(), any())).thenReturn(
                WCGatewayModel(
                    id = "woocommerce_payments",
                    title = "WooPayments (Card)",
                    description = "",
                    order = 0,
                    isEnabled = true,
                    methodTitle = "WooPayments",
                    methodDescription = "",
                    features = listOf("refunds")
                )
            )
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = "eftpos_au",
                    cardLast4 = "0978",
                    paymentMethodType = "card_present"
                )
            )

            initViewModel()

            val viewState = viewModel.refundSummaryStateLiveData.liveData.getOrAwaitValue()

            assertThat(viewState.refundMethod).isEqualTo("WooPayments (Card) (eftpos **** 0978)")
        }
    }

    @Test
    fun `given interac refund, when refund confirmed, then trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.firstOrNull()).isInstanceOf(RefundSummaryViewModel.NavigateToCardReaderScreen::class.java)
        }
    }

    @Test
    fun `given interac refund, when refund confirmed, then snack bar is not triggered`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.any()).isNotInstanceOf(
                MultiLiveEvent.Event.ShowSnackbar::class.java
            )
        }
    }

    @Test
    fun `given non interac refund, when refund confirmed, then snack bar is triggered with refund message`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat((events.firstOrNull() as? MultiLiveEvent.Event.ShowSnackbar)?.message).isEqualTo(
                R.string.order_refunds_amount_refund_progress_message
            )
        }
    }

    @Test
    fun `given interac refund, when initiating refund, then trigger updating backend snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events.firstOrNull()).isEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund
                )
            )
        }
    }

    @Test
    fun `given non-interac refund, when initiating refund, then don't trigger updating backend snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events.any()).isNotEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund
                )
            )
        }
    }

    @Test
    fun `given interac refund, when initiating refund fails, then trigger updating backend failed snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    amount = anyOrNull(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events[1]).isEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund_failed
                )
            )
        }
    }

    @Test
    fun `given non-interac refund, when initiating refund fails, then don't trigger update failed snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    amount = anyOrNull(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events.any()).isNotEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund_failed
                )
            )
        }
    }

    @Test
    fun `given non-interac refund, when refund() fails, then trigger failed snackbar`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    amount = anyOrNull(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            assertThat(events.firstOrNull()).isEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(
                    R.string.order_refunds_amount_refund_error
                )
            )
        }
    }

    @Test
    fun `given non-interac refund, when refund confirmed, then do not trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "card_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.firstOrNull()).isNotInstanceOf(
                RefundSummaryViewModel.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given charges call fails, when refund confirmed, then do not trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.firstOrNull()).isNotInstanceOf(
                RefundSummaryViewModel.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given non cash order, when charge data loaded with error, then card info is not visible`() {
        testBlocking {
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "stripe",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()

            val viewState = viewModel.refundSummaryStateLiveData.liveData.getOrAwaitValue()

            assertThat(viewState.refundMethod).isEqualTo("Credit/Debit card")
        }
    }

    @Test
    fun `given non cash order, when charge data loaded, then button enabled`() {
        testBlocking {
            val orderWithNonCash = createTestOrder(
                paymentMethod = "stripe",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithNonCash)
            initViewModel()

            val viewState = viewModel.refundSummaryStateLiveData.liveData.getOrAwaitValue()

            assertThat(viewState.isSubmitButtonEnabled).isTrue()
        }
    }

    @Test
    fun `given non cash order and text summary to long, when charge data loaded, then button not enabled`() {
        testBlocking {
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "stripe",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)

            initViewModel()

            viewModel.onRefundSummaryTextChanged(10, 100)

            val viewState = viewModel.refundSummaryStateLiveData.liveData.getOrAwaitValue()

            assertThat(viewState.isSubmitButtonEnabled).isFalse()
        }
    }

    @Test
    fun `given non cash order and non charge id in order, when charge data loading, then card info is not visible`() {
        testBlocking {
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "stripe",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()

            val viewState = viewModel.refundSummaryStateLiveData.liveData.getOrAwaitValue()

            assertThat(viewState.refundMethod).isEqualTo("Credit/Debit card")
        }
    }

    @Test
    fun `given there is network connection, when refund is confirmed, then proper tracks event is triggered`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val state = viewModel.refundSummaryStateLiveData.liveData.getOrAwaitValue()
            viewModel.onRefundConfirmed(true)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.REFUND_CREATE,
                mapOf(
                    AnalyticsTracker.KEY_ORDER_ID to ORDER_ID,
                    AnalyticsTracker.KEY_REFUND_IS_FULL to false.toString(),
                    AnalyticsTracker.KEY_REFUND_METHOD to "manual",
                    AnalyticsTracker.KEY_AMOUNT to (state).refundAmount.toString()
                )
            )
        }
    }

    @Test
    fun `given there is no network connection, when refund is confirmed, then tracks event is not triggered`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(networkStatus.isConnected()).thenReturn(false)
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            viewModel.onRefundConfirmed(true)

            verify(analyticsTrackerWrapper, never()).track(any(), any())
        }
    }

    @Test
    fun `when refund error, then proper tracks event is triggered`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    amount = anyOrNull(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    error = WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = BaseRequest.GenericErrorType.NETWORK_ERROR
                    )
                )
            )

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.refund()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.REFUND_CREATE_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_ORDER_ID to ORDER_ID,
                    AnalyticsTracker.KEY_ERROR_CONTEXT to RefundSummaryViewModel::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to "GENERIC_ERROR",
                    AnalyticsTracker.KEY_ERROR_DESC to null
                )
            )
        }
    }

    @Test
    fun `when refund success, then proper tracks event is triggered`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
                chargeId = chargeId,
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success(
                    cardBrand = cardBrand,
                    cardLast4 = cardLast4,
                    paymentMethodType = "interac_present"
                )
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    amount = anyOrNull(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    model = WCRefundModel(
                        id = 1L,
                        dateCreated = Date(),
                        amount = BigDecimal.ZERO,
                        reason = "",
                        automaticGatewayRefund = false,
                        items = listOf(),
                        shippingLineItems = listOf(),
                        feeLineItems = listOf()
                    )
                )
            )

            initViewModel()
            viewModel.refund()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.REFUND_CREATE_SUCCESS,
                mapOf(
                    AnalyticsTracker.KEY_ORDER_ID to ORDER_ID,
                    AnalyticsTracker.KEY_ID to 1L
                )
            )
        }
    }

    @Test
    fun `given refunded products, when refund succeeds, then only refunded product stock change is signalled`() {
        testBlocking {
            val refundedProductId = 101L
            val notRefundedProductId = 202L
            val refundedItem = ProductRefundListItem(
                orderItem = OrderTestUtils.generateTestOrderItems(productId = refundedProductId).first(),
                quantity = 1
            )
            val notRefundedItem = ProductRefundListItem(
                orderItem = OrderTestUtils.generateTestOrderItems(productId = notRefundedProductId).first(),
                quantity = 0
            )
            val order = createTestOrder(paymentMethod = "cod")
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(order)
            whenever(
                refundStore.createItemsRefund(
                    site = any(),
                    orderId = any(),
                    amount = anyOrNull(),
                    reason = any(),
                    restockItems = any(),
                    autoRefund = any(),
                    items = any()
                )
            ).thenReturn(
                WooResult(
                    model = WCRefundModel(
                        id = 1L,
                        dateCreated = Date(),
                        amount = BigDecimal.ZERO,
                        reason = "",
                        automaticGatewayRefund = false,
                        items = listOf(),
                        shippingLineItems = listOf(),
                        feeLineItems = listOf()
                    )
                )
            )

            initViewModel(refundItems = arrayOf(refundedItem, notRefundedItem))
            viewModel.refund()

            verify(refreshProductsSignal).notifyProductsChanged(listOf(refundedProductId))
        }
    }

    @Test
    fun `when refund is issued, then proper track event is triggered`() {
        testBlocking {
            val orderWithMultipleShipping = createTestOrder(
                paymentMethod = "cod",
            )
            whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(orderWithMultipleShipping)
            whenever(resourceProvider.getString(any())).thenReturn("")

            initViewModel()
            viewModel.onRefundIssued("")

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_ORDER_ID to ORDER_ID
                )
            )
        }
    }

    private fun createTestOrder(
        paymentMethod: String = "cod",
        chargeId: String = "charge_id",
    ): Order = mock {
        on { id } doReturn ORDER_ID
        on { this.paymentMethod } doReturn paymentMethod
        on { this.chargeId } doReturn chargeId
        on { currency } doReturn "USD"
        on { maxRefund } doReturn BigDecimal("50.00")
        on { refundTotal } doReturn BigDecimal.ZERO
    }
}
