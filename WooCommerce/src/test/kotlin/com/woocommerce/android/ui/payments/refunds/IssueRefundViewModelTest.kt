package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.RefundByItemsViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class IssueRefundViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
    }

    private val orderStore: WCOrderStore = mock()
    private val wooStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val gatewayStore: WCGatewayStore = mock()
    private val refundStore: WCRefundStore = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on(it.getString(R.string.multiple_shipping)).thenAnswer { "Multiple shipping lines" }
        on(it.getString(R.string.and)).thenAnswer { "and" }
        on(it.getString(any(), any())).thenAnswer { i ->
            "You can refund " + i.arguments[1].toString()
        }
    }
    private val orderMapper = OrderMapper(
        getLocations = mock {
            on { invoke(any(), any()) } doReturn (Location.EMPTY to AmbiguousLocation.EMPTY)
        },
        mock()
    )

    private val paymentChargeRepository: PaymentChargeRepository = mock()

    private val savedState = IssueRefundFragmentArgs(ORDER_ID).toSavedStateHandle()

    private lateinit var viewModel: IssueRefundViewModel

    private fun initViewModel() {
        whenever(selectedSite.get()).thenReturn(SiteModel())
        whenever(currencyFormatter.buildBigDecimalFormatter(any())).thenReturn { "" }

        viewModel = IssueRefundViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            currencyFormatter,
            orderStore,
            wooStore,
            selectedSite,
            networkStatus,
            resourceProvider,
            orderDetailRepository,
            gatewayStore,
            refundStore,
            paymentChargeRepository,
            orderMapper,
            analyticsTrackerWrapper,
        )
    }

    @Test
    fun `given order has only custom amt, when refund button clicked, then refund custom amount toggle is enabled`() {
        testBlocking {
            val orderWithCustomAmount = OrderTestUtils.generateOrderWithCustomAmount()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithCustomAmount)

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            viewState!!.isFeesRefundAvailable?.let { assertTrue(it) }
            assertTrue(viewState!!.isFeesMainSwitchChecked)
        }
    }

    @Test
    fun `when order has no shipping, then refund notice is not visible`() {
        testBlocking {
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(OrderTestUtils.generateOrder())

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertFalse(viewState!!.isRefundNoticeVisible)
        }
    }

    @Test
    fun `when order has one shipping, then the notice not visible`() {
        testBlocking {
            val orderWithShipping = OrderTestUtils.generateOrderWithOneShipping()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithShipping)

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertFalse(viewState!!.isRefundNoticeVisible)
        }
    }

    @Test
    fun `when order has multiple shipping, multiple shipping are mentioned in the notice`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertTrue(viewState!!.isRefundNoticeVisible)
            assertEquals("You can refund multiple shipping lines", viewState!!.refundNotice)
        }
    }

    @Test
    fun `given non cash order, when successfully charge data loaded, then card info is visible`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.refundMethod).isEqualTo("Credit/Debit card (Visa **** 1234)")
        }
    }

    @Test
    fun `given interac refund, when refund confirmed, then trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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

            assertThat(events.first()).isInstanceOf(
                IssueRefundViewModel.IssueRefundEvent.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given interac refund, when refund confirmed, then snack bar is not triggered`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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

            assertThat((events.first() as MultiLiveEvent.Event.ShowSnackbar).message).isEqualTo(
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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

            assertThat(events.first()).isEqualTo(
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
                    amount = any(),
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
                    amount = any(),
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
                    amount = any(),
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

            assertThat(events.first()).isEqualTo(
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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

            assertThat(events.first()).isNotInstanceOf(
                IssueRefundViewModel.IssueRefundEvent.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given charges call fails, when refund confirmed, then do not trigger card reader screen`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            viewModel.event.observeForever { events.add(it) }
            viewModel.onRefundConfirmed(true)

            assertThat(events.first()).isNotInstanceOf(
                IssueRefundViewModel.IssueRefundEvent.NavigateToCardReaderScreen::class.java
            )
        }
    }

    @Test
    fun `given non cash order, when charge data loaded with error, then card info is not visible`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.refundMethod).isEqualTo("Credit/Debit card")
        }
    }

    @Test
    fun `given non cash order, when charge data loaded, then button enabled`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )

            initViewModel()

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.isSubmitButtonEnabled).isTrue()
        }
    }

    @Test
    fun `given non cash order and text summary to long, when charge data loaded, then button not enabled`() {
        testBlocking {
            val chargeId = "charge_id"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)).thenReturn(
                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error
            )

            initViewModel()

            viewModel.onRefundSummaryTextChanged(10, 100)

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.isSubmitButtonEnabled).isFalse()
        }
    }

    @Test
    fun `given non cash order and non charge id in order, when charge data loading, then card info is not visible`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()

            var viewState: IssueRefundViewModel.RefundSummaryViewState? = null
            viewModel.refundSummaryStateLiveData.observeForever { _, new -> viewState = new }

            assertThat(viewState!!.refundMethod).isEqualTo("Credit/Debit card")
        }
    }

    @Test
    fun `when next button is tapped from items, then verify proper tracks event is triggered `() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(resourceProvider.getString(R.string.order_refunds_manual_refund))
                .thenReturn("Credit/Debit card")

            initViewModel()
            viewModel.onNextButtonTappedFromItems()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_REFUND_TYPE to IssueRefundViewModel.RefundType.ITEMS.name,
                    AnalyticsTracker.KEY_ORDER_ID to ORDER_ID
                )
            )
        }
    }

    @Test
    fun `when next button is tapped from amounts, then verify proper tracks event is triggered `() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(resourceProvider.getString(any())).thenReturn("")

            initViewModel()
            viewModel.onNextButtonTappedFromAmounts()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_REFUND_TYPE to IssueRefundViewModel.RefundType.AMOUNT.name,
                    AnalyticsTracker.KEY_ORDER_ID to ORDER_ID
                )
            )
        }
    }

    @Test
    fun `given there is network connection, when refund is confirmed, then proper tracks event is triggered`() {
        testBlocking {
            val chargeId = "charge_id"
            val cardBrand = "visa"
            val cardLast4 = "1234"
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(true)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
            val commonState = viewModel.commonStateLiveData.liveData.value as IssueRefundViewModel.CommonViewState
            viewModel.onRefundConfirmed(true)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.REFUND_CREATE,
                mapOf(
                    AnalyticsTracker.KEY_ORDER_ID to ORDER_ID,
                    AnalyticsTracker.KEY_REFUND_IS_FULL to
                        ((commonState).refundTotal isEqualTo BigDecimal.TEN).toString(),
                    AnalyticsTracker.KEY_REFUND_TYPE to (commonState).refundType.name,
                    AnalyticsTracker.KEY_REFUND_METHOD to "manual",
                    AnalyticsTracker.KEY_AMOUNT to (commonState).refundTotal.toString()
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(networkStatus.isConnected()).thenReturn(false)
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
                    amount = any(),
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
                    AnalyticsTracker.KEY_ERROR_CONTEXT to "IssueRefundViewModel",
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
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[{\"key\"=\"_charge_id\", \"value\"=\"$chargeId\"}]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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
                    amount = any(),
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
    fun `when refund is issued, then proper track event is triggered`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
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

    @Test
    fun `when refund quantity is tapped, then proper track event is triggered`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)

            initViewModel()
            viewModel.onRefundQuantityTapped(1L)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to ORDER_ID)
            )
        }
    }

    @Test
    fun `when product refund amount is tapped, then proper track event is triggered`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)

            initViewModel()
            viewModel.onProductRefundAmountTapped()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_PRODUCT_AMOUNT_DIALOG_OPENED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to ORDER_ID)
            )
        }
    }

    @Test
    fun `when select button is tapped, then proper track event is triggered`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = "[]"
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)

            initViewModel()
            viewModel.onSelectButtonTapped()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to ORDER_ID)
            )
        }
    }
}
