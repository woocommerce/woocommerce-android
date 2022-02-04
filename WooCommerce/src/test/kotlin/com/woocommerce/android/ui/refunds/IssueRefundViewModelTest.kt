package com.woocommerce.android.ui.refunds

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundByItemsViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class IssueRefundViewModelTest : BaseUnitTest() {
    private val orderStore: WCOrderStore = mock()
    private val wooStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val gatewayStore: WCGatewayStore = mock()
    private val refundStore: WCRefundStore = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val resourceProvider: ResourceProvider = mock {
        on(it.getString(R.string.taxes)).thenAnswer { "Taxes" }
        on(it.getString(R.string.multiple_shipping)).thenAnswer { "Multiple shipping lines" }
        on(it.getString(R.string.and)).thenAnswer { "and" }
        on(it.getString(any(), any())).thenAnswer { i ->
            "You can refund " + i.arguments[1].toString()
        }
    }
    private val orderMapper = OrderMapper(
        getLocations = mock {
            on { invoke(any(), any()) } doReturn (Location.EMPTY to AmbiguousLocation.EMPTY)
        }
    )

    private val paymentChargeRepository: PaymentChargeRepository = mock()

    private val savedState = IssueRefundFragmentArgs(0).initSavedStateHandle()

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
            orderMapper
        )
    }

    @Test
    fun `when order has zero taxes and no shipping and fees, then refund notice is not visible`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(OrderTestUtils.generateOrder())

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertFalse(viewState!!.isRefundNoticeVisible)
        }
    }

    @Test
    fun `when order has taxes and no shipping and fees, then only the taxes are mentioned in the notice`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val orderWithTax = OrderTestUtils.generateOrder().copy(totalTax = "4.00")
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithTax)

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertTrue(viewState!!.isRefundNoticeVisible)
            assertEquals("You can refund taxes", viewState!!.refundNotice)
        }
    }

    @Test
    fun `when order has one shipping and fees without taxes, then the notice not visible`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val orderWithFeesAndShipping = OrderTestUtils.generateOrderWithFee()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithFeesAndShipping)

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertFalse(viewState!!.isRefundNoticeVisible)
        }
    }

    @Test
    fun `when order has one shipping, and fees and taxes, then taxes are mentioned in the notice`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val orderWithFeesAndShipping = OrderTestUtils.generateOrderWithFee().copy(totalTax = "4.00")
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithFeesAndShipping)

            initViewModel()

            var viewState: RefundByItemsViewState? = null
            viewModel.refundByItemsStateLiveData.observeForever { _, new -> viewState = new }

            assertTrue(viewState!!.isRefundNoticeVisible)
            assertEquals("You can refund taxes", viewState!!.refundNotice)
        }
    }

    @Test
    fun `when order has multiple shipping, multiple shipping are mentioned in the notice`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
                    cardLast4 = cardLast4
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
    fun `given non cash order, when charge data loaded with error, then card info is not visible`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `given non cash order and non charge id in order, when charge data loading, then card info is not visible`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
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
}
