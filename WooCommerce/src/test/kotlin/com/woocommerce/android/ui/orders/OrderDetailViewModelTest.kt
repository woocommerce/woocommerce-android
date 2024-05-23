package com.woocommerce.android.ui.orders

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.GiftCardSummary
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.model.Subscription
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.network.giftcard.GiftCardRestClient
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.giftcard.GiftCardRepository
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PreviewReceipt
import com.woocommerce.android.ui.orders.creation.shipping.GetShippingMethodsWithOtherValue
import com.woocommerce.android.ui.orders.creation.shipping.RefreshShippingMethods
import com.woocommerce.android.ui.orders.details.GetOrderSubscriptions
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.details.OrderDetailTracker
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.orders.details.OrderDetailViewState
import com.woocommerce.android.ui.orders.details.OrderDetailViewState.OrderInfo
import com.woocommerce.android.ui.orders.details.OrderDetailsTransactionLauncher
import com.woocommerce.android.ui.orders.details.OrderProduct
import com.woocommerce.android.ui.orders.details.OrderProductMapper
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.UseConstructor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderAttributionInfo
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.CancellationException

@ExperimentalCoroutinesApi
class OrderDetailViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private const val ORDER_SITE_ID = 1
    }

    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val networkStatus: NetworkStatus = mock()
    private val appPrefsWrapper: AppPrefs = mock {
        on(it.isTrackingExtensionAvailable()).thenAnswer { true }
    }
    private val editor = mock<SharedPreferences.Editor>()
    private val preferences = mock<SharedPreferences> { whenever(it.edit()).thenReturn(editor) }
    private val selectedSite: SelectedSite = mock()
    private val pluginsInfo = HashMap<String, WooPlugin>()
    private val orderDetailRepository: OrderDetailRepository = mock {
        onBlocking { getOrderDetailsPluginsInfo() } doReturn pluginsInfo
    }
    private val addonsRepository: AddonRepository = mock()
    private val paymentsFlowTracker: PaymentsFlowTracker = mock()
    private val orderDetailTracker: OrderDetailTracker = mock()
    private val resources: ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
    }
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()
    private val shippingLabelOnboardingRepository: ShippingLabelOnboardingRepository = mock {
        doReturn(true).whenever(it).isShippingPluginReady
    }

    private val savedState = OrderDetailFragmentArgs(
        orderId = ORDER_ID,
        allOrderIds = arrayOf(ORDER_ID).toLongArray()
    ).toSavedStateHandle()

    private val productImageMap = mock<ProductImageMap>()
    private val orderDetailsTransactionLauncher = mock<OrderDetailsTransactionLauncher>()
    private val orderProductMapper = OrderProductMapper()
    private val productDetailRepository: ProductDetailRepository = mock()
    private val paymentReceiptHelper: PaymentReceiptHelper = mock {
        onBlocking { isReceiptAvailable(any()) }.thenReturn(false)
        onBlocking { getReceiptUrl(any()) }.thenReturn(Result.success("https://www.testname.com"))
    }

    private val order = OrderTestUtils.generateTestOrder(ORDER_ID)
    private val orderInfo = OrderInfo(OrderTestUtils.generateTestOrder(ORDER_ID))
    private val orderStatus = OrderStatus(order.status.value, order.status.value)
    private val testOrderNotes = OrderTestUtils.generateTestOrderNotes(
        totalNotes = 5,
        orderId = ORDER_ID
    )
    private val testOrderShipmentTrackings = OrderTestUtils.generateTestOrderShipmentTrackings(
        totalCount = 5,
        orderId = ORDER_ID,
        localSiteId = ORDER_SITE_ID,
    )
    private val orderShippingLabels = OrderTestUtils.generateShippingLabels(totalCount = 5)
    private val testOrderRefunds = OrderTestUtils.generateRefunds(1)
    private lateinit var viewModel: OrderDetailViewModel

    private val currentViewStateValue
        get() = viewModel.viewStateData.liveData.value

    private val orderWithParameters = OrderDetailViewState(
        orderInfo = orderInfo,
        toolbarTitle = resources.getString(string.orderdetail_orderstatus_ordernum, order.number),
        isShipmentTrackingAvailable = true,
        isCreateShippingLabelButtonVisible = false,
        isProductListVisible = true,
        areShippingLabelsVisible = false,
        isProductListMenuVisible = false,
        wcShippingBannerVisible = false,
        isRefreshing = false,
        isOrderDetailSkeletonShown = false
    )

    private val getOrderSubscriptions: GetOrderSubscriptions = mock()
    private val giftCardRestClient: GiftCardRestClient = mock()
    private val giftCardRepository: GiftCardRepository = mock(
        useConstructor = UseConstructor.withArguments(
            selectedSite,
            giftCardRestClient,
            coroutinesTestRule.testDispatchers
        )
    )

    private val getShippingMethodsWithOtherValue: GetShippingMethodsWithOtherValue = mock()
    private val refreshShippingMethods: RefreshShippingMethods = mock()

    private fun createViewModel() {
        createViewModel(newSavedState = savedState)
    }

    private fun createViewModel(newSavedState: SavedStateHandle) {
        viewModel = spy(
            OrderDetailViewModel(
                newSavedState,
                appPrefsWrapper,
                networkStatus,
                resources,
                orderDetailRepository,
                addonsRepository,
                selectedSite,
                productImageMap,
                paymentCollectibilityChecker,
                paymentsFlowTracker,
                orderDetailTracker,
                shippingLabelOnboardingRepository,
                orderDetailsTransactionLauncher,
                getOrderSubscriptions,
                giftCardRepository,
                orderProductMapper,
                productDetailRepository,
                paymentReceiptHelper,
                analyticsTracker,
                refreshShippingMethods,
                getShippingMethodsWithOtherValue,
            )
        )
    }

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()

        val site = SiteModel().let {
            it.id = 1
            it.siteId = 1
            it.selfHostedSiteId = 1
            it.name = "https://www.testname.com"
            it
        }
        doReturn(site).whenever(selectedSite).getIfExists()
        testBlocking {
            doReturn(false).whenever(paymentCollectibilityChecker).isCollectable(any())
        }

        pluginsInfo.clear()

        createViewModel()

        clearInvocations(
            viewModel,
            selectedSite,
            orderDetailRepository,
            networkStatus,
            resources
        )

        mock<Context> {
            whenever(it.applicationContext).thenReturn(it)
            whenever(it.getSharedPreferences(any(), any())).thenReturn(preferences)
            AppPrefs.init(it)
        }
    }

    @Test
    fun `Displays the order detail view correctly`() = testBlocking {
        val nonRefundedOrder = order.copy(refundTotal = BigDecimal.ZERO)

        val expectedViewState = orderWithParameters.copy(
            orderInfo = orderInfo.copy(order = nonRefundedOrder)
        )

        doReturn(false).whenever(paymentCollectibilityChecker).isCollectable(any())

        doReturn(nonRefundedOrder).whenever(orderDetailRepository).getOrderById(any())

        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())

        doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

        doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

        doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

        var orderData: OrderDetailViewState? = null
        viewModel.viewStateData.observeForever { _, new -> orderData = new }

        // order notes
        val orderNotes = ArrayList<OrderNote>()
        viewModel.orderNotes.observeForever {
            it?.let {
                orderNotes.clear()
                orderNotes.addAll(it)
            }
        }

        // order shipment Trackings
        val shipmentTrackings = ArrayList<OrderShipmentTracking>()
        viewModel.shipmentTrackings.observeForever {
            it?.let {
                shipmentTrackings.clear()
                shipmentTrackings.addAll(it)
            }
        }

        // product list should not be empty when shipping labels are not available and products are not refunded
        val products = ArrayList<OrderProduct>()
        viewModel.productList.observeForever {
            it?.let { products.addAll(it) }
        }

        // refunds
        val refunds = ArrayList<Refund>()
        viewModel.orderRefunds.observeForever {
            it?.let { refunds.addAll(it) }
        }

        // shipping Labels
        val shippingLabels = ArrayList<ShippingLabel>()
        viewModel.shippingLabels.observeForever {
            it?.let { shippingLabels.addAll(it) }
        }

        viewModel.start()

        assertThat(orderData).isEqualTo(expectedViewState)

        assertThat(orderNotes).isNotEmpty
        assertThat(orderNotes).isEqualTo(testOrderNotes)

        assertThat(shipmentTrackings).isNotEmpty
        assertThat(shipmentTrackings).isEqualTo(testOrderShipmentTrackings)

        assertThat(products).isNotEmpty
        assertThat(refunds).isEmpty()
        assertThat(shippingLabels).isEmpty()
    }

    @Test
    fun `given receipt is available and order is paid, when view model started, then state with receipt is visible emitted`() =
        testBlocking {
            // GIVEN
            whenever(paymentReceiptHelper.isReceiptAvailable(any())).thenReturn(true)
            whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(false)
            whenever(orderDetailRepository.getOrderById(any())).thenReturn(
                order.copy(
                    datePaid = Date()
                )
            )
            whenever(orderDetailRepository.fetchOrderNotes(any())).thenReturn(true)
            whenever(orderDetailRepository.getOrderNotes(any())).thenReturn(testOrderNotes)
            whenever(orderDetailRepository.getOrderShipmentTrackings(any())).thenReturn(testOrderShipmentTrackings)
            whenever(orderDetailRepository.getOrderRefunds(any())).thenReturn(emptyList())
            whenever(orderDetailRepository.getOrderShippingLabels(any())).thenReturn(emptyList())
            whenever(addonsRepository.containsAddonsFrom(any())).thenReturn(false)

            // WHEN
            var detailViewState: OrderDetailViewState? = null
            viewModel.viewStateData.observeForever { _, new -> detailViewState = new }

            viewModel.start()

            // THEN
            assertThat(detailViewState!!.orderInfo!!.receiptButtonStatus).isEqualTo(
                OrderDetailViewState.ReceiptButtonStatus.Visible
            )
        }

    @Test
    fun `given receipt is available and order not paid, when view model started, then state with receipt is hidden emitted`() =
        testBlocking {
            // GIVEN
            whenever(paymentReceiptHelper.isReceiptAvailable(any())).thenReturn(true)
            whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(true)
            whenever(orderDetailRepository.getOrderById(any())).thenReturn(
                order.copy(
                    datePaid = null
                )
            )
            whenever(orderDetailRepository.fetchOrderNotes(any())).thenReturn(true)
            whenever(orderDetailRepository.getOrderNotes(any())).thenReturn(testOrderNotes)
            whenever(orderDetailRepository.getOrderShipmentTrackings(any())).thenReturn(testOrderShipmentTrackings)
            whenever(orderDetailRepository.getOrderRefunds(any())).thenReturn(emptyList())
            whenever(orderDetailRepository.getOrderShippingLabels(any())).thenReturn(emptyList())
            whenever(addonsRepository.containsAddonsFrom(any())).thenReturn(false)

            // WHEN
            var detailViewState: OrderDetailViewState? = null
            viewModel.viewStateData.observeForever { _, new -> detailViewState = new }

            viewModel.start()

            // THEN
            assertThat(detailViewState!!.orderInfo!!.receiptButtonStatus).isEqualTo(
                OrderDetailViewState.ReceiptButtonStatus.Hidden
            )
        }

    @Test
    fun `given receipt is not available, when view model started, then state with receipt is hidden emitted`() =
        testBlocking {
            // GIVEN
            whenever(paymentReceiptHelper.isReceiptAvailable(any())).thenReturn(false)
            whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(false)
            whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
            whenever(orderDetailRepository.fetchOrderNotes(any())).thenReturn(true)
            whenever(orderDetailRepository.getOrderNotes(any())).thenReturn(testOrderNotes)
            whenever(orderDetailRepository.getOrderShipmentTrackings(any())).thenReturn(testOrderShipmentTrackings)
            whenever(orderDetailRepository.getOrderRefunds(any())).thenReturn(emptyList())
            whenever(orderDetailRepository.getOrderShippingLabels(any())).thenReturn(emptyList())
            whenever(addonsRepository.containsAddonsFrom(any())).thenReturn(false)

            // WHEN
            var detailViewState: OrderDetailViewState? = null
            viewModel.viewStateData.observeForever { _, new -> detailViewState = new }

            viewModel.start()

            // THEN
            assertThat(detailViewState!!.orderInfo!!.receiptButtonStatus).isEqualTo(
                OrderDetailViewState.ReceiptButtonStatus.Hidden
            )
        }

    @Test
    fun `collect button hidden if payment is not collectable`() =
        testBlocking {
            // GIVEN
            doReturn(false).whenever(paymentCollectibilityChecker).isCollectable(any())
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            // WHEN
            viewModel.start()

            // THEN
            assertThat(currentViewStateValue!!.orderInfo!!.isPaymentCollectableWithCardReader).isFalse()
        }

    @Test
    fun `collect button shown if payment is collectable`() =
        testBlocking {
            // GIVEN
            doReturn(true).whenever(paymentCollectibilityChecker).isCollectable(any())
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            // WHEN
            viewModel.start()

            // THEN
            assertThat(currentViewStateValue!!.orderInfo!!.isPaymentCollectableWithCardReader).isTrue()
        }

    @Test
    fun `hasVirtualProductsOnly returns false if there are no products for the order`() =
        testBlocking {
            val order = order.copy(items = emptyList())
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())

            viewModel.start()
            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(false)
        }

    @Test
    fun `hasVirtualProductsOnly returns true if and only if there are no physical products for the order`() =
        testBlocking {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val virtualItems = listOf(item.copy(productId = 3), item.copy(productId = 4))
            val virtualOrder = order.copy(items = virtualItems)

            doReturn(true).whenever(orderDetailRepository).hasVirtualProductsOnly(listOf(3, 4))
            doReturn(virtualOrder).whenever(orderDetailRepository).getOrderById(any())
            doReturn(virtualOrder).whenever(orderDetailRepository).fetchOrderById(any())

            doReturn(testOrderRefunds).whenever(orderDetailRepository).getOrderRefunds(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())

            viewModel.start()

            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(true)
        }

    @Test
    fun `hasVirtualProductsOnly returns false if there are both virtual and physical products for the order`() =
        testBlocking {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val mixedItems = listOf(item, item.copy(productId = 2))
            val mixedOrder = order.copy(items = mixedItems)

            doReturn(false).whenever(orderDetailRepository).hasVirtualProductsOnly(listOf(1, 2))
            doReturn(mixedOrder).whenever(orderDetailRepository).getOrderById(any())
            doReturn(mixedOrder).whenever(orderDetailRepository).fetchOrderById(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            viewModel.start()

            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(false)
        }

    @Test
    fun `don't fetch products if we have all products`() =
        testBlocking {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val items = listOf(item, item.copy(productId = 2))
            val ids = items.map { it.productId }

            val order = order.copy(items = items)
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(ids.size).whenever(orderDetailRepository).getProductCountForOrder(any())
            viewModel.start()

            verify(orderDetailRepository, never()).fetchProductsByRemoteIds(ids)
        }

    @Test
    fun `fetch products if there are some missing`() =
        testBlocking {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val items = listOf(item, item.copy(productId = 2))
            val ids = items.map { it.productId }

            val order = order.copy(items = items)
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(1).whenever(orderDetailRepository).getProductCountForOrder(ids)

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            viewModel.start()

            verify(orderDetailRepository, atLeastOnce()).fetchProductsByRemoteIds(ids)
        }

    @Test
    fun `Do not display product list when all products are refunded`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

            doReturn(testOrderRefunds).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(testOrderRefunds).whenever(orderDetailRepository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())

            val refunds = ArrayList<Refund>()
            viewModel.orderRefunds.observeForever {
                it?.let { refunds.addAll(it) }
            }

            var areProductsVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                areProductsVisible = new.isProductListVisible
            }

            viewModel.start()

            assertThat(areProductsVisible).isFalse
            assertThat(refunds).isNotEmpty
        }

    @Test
    fun `Display product list when shipping labels are available`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

            doReturn(orderShippingLabels).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            val shippingLabels = ArrayList<ShippingLabel>()
            viewModel.shippingLabels.observeForever {
                it?.let { shippingLabels.addAll(it) }
            }

            var areProductsVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                areProductsVisible = new.isProductListVisible
            }

            viewModel.start()

            assertThat(shippingLabels).isNotEmpty
            assertThat(areProductsVisible).isTrue()
        }

    @Test
    fun `Hide Create shipping label button and show Products area menu when shipping labels are available`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

            doReturn(orderShippingLabels).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            doReturn(Unit).whenever(orderDetailRepository).fetchSLCreationEligibility(order.id)
            doReturn(true).whenever(orderDetailRepository).isOrderEligibleForSLCreation(order.id)
            doReturn(true).whenever(shippingLabelOnboardingRepository).isShippingPluginReady

            val shippingLabels = ArrayList<ShippingLabel>()
            viewModel.shippingLabels.observeForever {
                it?.let { shippingLabels.addAll(it) }
            }

            var isCreateShippingLabelButtonVisible: Boolean? = null
            var isProductListMenuVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
                isProductListMenuVisible = new.isProductListMenuVisible
            }

            viewModel.start()

            assertThat(shippingLabels).isNotEmpty
            assertThat(isCreateShippingLabelButtonVisible).isFalse
            assertThat(isProductListMenuVisible).isTrue
        }

    @Test
    fun `Show Create shipping label button and hide Products area menu when no shipping labels are available`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            doReturn(Unit).whenever(orderDetailRepository).fetchSLCreationEligibility(order.id)
            doReturn(true).whenever(orderDetailRepository).isOrderEligibleForSLCreation(order.id)
            doReturn(true).whenever(shippingLabelOnboardingRepository).isShippingPluginReady

            val shippingLabels = ArrayList<ShippingLabel>()
            viewModel.shippingLabels.observeForever {
                it?.let { shippingLabels.addAll(it) }
            }

            var isCreateShippingLabelButtonVisible: Boolean? = null
            var isProductListMenuVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
                isProductListMenuVisible = new.isProductListMenuVisible
            }

            viewModel.start()

            assertThat(shippingLabels).isEmpty()
            assertThat(isCreateShippingLabelButtonVisible).isTrue
            assertThat(isProductListMenuVisible).isFalse
        }

    @Test
    fun `Do not display shipment tracking when shipping labels are available`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

            doReturn(orderShippingLabels).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            var orderData: OrderDetailViewState? = null
            viewModel.viewStateData.observeForever { _, new -> orderData = new }

            val shippingLabels = ArrayList<ShippingLabel>()
            viewModel.shippingLabels.observeForever {
                it?.let { shippingLabels.addAll(it) }
            }

            viewModel.start()

            assertThat(shippingLabels).isNotEmpty
            assertThat(orderData?.isShipmentTrackingAvailable).isFalse()
        }

    @Test
    fun `Do not display shipment tracking when order is eligible for in-person payments`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(paymentCollectibilityChecker).isCollectable(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            val shippingLabels = ArrayList<ShippingLabel>()
            viewModel.shippingLabels.observeForever {
                it?.let { shippingLabels.addAll(it) }
            }

            var isCreateShippingLabelButtonVisible: Boolean? = null
            var isProductListMenuVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
                isProductListMenuVisible = new.isProductListMenuVisible
            }

            viewModel.start()

            assertThat(shippingLabels).isEmpty()
            assertThat(isCreateShippingLabelButtonVisible).isFalse
            assertThat(isProductListMenuVisible).isFalse
        }

    @Test
    fun `Display error message on fetch order error`() = testBlocking {
        whenever(orderDetailRepository.fetchOrderById(ORDER_ID)).thenReturn(null)
        whenever(orderDetailRepository.getOrderById(ORDER_ID)).thenReturn(null)
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(orderDetailRepository, times(1)).fetchOrderById(ORDER_ID)
        verify(viewModel, never()).order

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.order_error_fetch_generic))
    }

    @Test
    fun `Shows and hides order detail skeleton correctly`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(null).whenever(orderDetailRepository).getOrderById(any())

            val isSkeletonShown = ArrayList<Boolean>()
            viewModel.viewStateData.observeForever { old, new ->
                new.isOrderDetailSkeletonShown?.takeIfNotEqualTo(old?.isOrderDetailSkeletonShown) {
                    isSkeletonShown.add(it)
                }
            }

            viewModel.start()
            assertThat(isSkeletonShown).containsExactly(true, false)
        }

    @Test
    fun `Do not fetch order from api when not connected`() = testBlocking {
        doReturn(null).whenever(orderDetailRepository).getOrderById(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(orderDetailRepository, times(1)).getOrderById(ORDER_ID)
        verify(orderDetailRepository, times(0)).fetchOrderById(any())

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `Update order status and handle undo action`() = testBlocking {
        val newOrderStatus = OrderStatus(CoreOrderStatus.PROCESSING.value, CoreOrderStatus.PROCESSING.value)

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
        doReturn(orderStatus).doReturn(newOrderStatus).doReturn(orderStatus).whenever(orderDetailRepository)
            .getOrderStatus(any())

        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

        doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
        doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

        doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
        doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

        var snackbar: ShowUndoSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowUndoSnackbar) snackbar = it
        }

        val orderStatusList = ArrayList<OrderStatus>()
        viewModel.viewStateData.observeForever { old, new ->
            new.orderStatus?.takeIfNotEqualTo(old?.orderStatus) { orderStatusList.add(it) }
        }

        val statusChangeCaptor = argumentCaptor<String>()

        viewModel.start()

        val initialStatus = order.status.value
        val newStatus = CoreOrderStatus.PROCESSING.value

        viewModel.onOrderStatusChanged(OrderStatusUpdateSource.Dialog(initialStatus, newStatus))
        assertThat(snackbar?.message).isEqualTo(resources.getString(string.order_status_updated))

        // simulate undo click event
        snackbar?.undoAction?.onClick(mock())
        assertThat(snackbar?.message).isEqualTo(resources.getString(string.order_status_updated))

        verify(orderDetailRepository, times(2)).updateOrderStatus(eq(order.id), statusChangeCaptor.capture())

        assertThat(listOf(initialStatus) + statusChangeCaptor.allValues).containsExactly(
            initialStatus,
            newStatus,
            initialStatus
        )
    }

    @Test
    fun `Update order status when network connected`() = testBlocking {
        val newOrderStatus = OrderStatus(CoreOrderStatus.PROCESSING.value, CoreOrderStatus.PROCESSING.value)

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
        doReturn(orderStatus).doReturn(newOrderStatus).whenever(orderDetailRepository).getOrderStatus(any())

        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(testOrderNotes).whenever(orderDetailRepository).getOrderNotes(any())

        doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
        doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

        doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
        doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

        var newOrder: Order? = null
        viewModel.viewStateData.observeForever { old, new ->
            new.orderInfo?.takeIfNotEqualTo(old?.orderInfo) { newOrder = it.order }
        }

        viewModel.start()
        viewModel.onOrderStatusChanged(
            OrderStatusUpdateSource.Dialog(
                oldStatus = newOrder!!.status.value,
                newStatus = CoreOrderStatus.PROCESSING.value
            )
        )

        assertThat(newOrder?.status).isEqualTo(order.status)
    }

    @Test
    fun `Do not update order status when not connected`() = testBlocking {
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.order = order
        viewModel.start()
        viewModel.onOrderStatusChanged(
            OrderStatusUpdateSource.Dialog(
                oldStatus = order.status.value,
                newStatus = CoreOrderStatus.PROCESSING.value
            )
        )

        verify(orderDetailRepository, never()).updateOrderStatus(any(), any())

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `refresh shipping tracking items when an item is added`() = testBlocking {
        val shipmentTracking = OrderShipmentTracking(
            trackingProvider = "testProvider",
            trackingNumber = "123456",
            dateShipped = DateUtils.getCurrentDateString()
        )

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())

        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())

        val addedShipmentTrackings = testOrderShipmentTrackings.toMutableList()
        addedShipmentTrackings.add(shipmentTracking)
        doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
        doReturn(testOrderShipmentTrackings).doReturn(addedShipmentTrackings)
            .whenever(orderDetailRepository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

        var orderShipmentTrackings = emptyList<OrderShipmentTracking>()
        viewModel.shipmentTrackings.observeForever {
            it?.let { orderShipmentTrackings = it }
        }

        viewModel.start()
        viewModel.onNewShipmentTrackingAdded(shipmentTracking)

        // (1) when order is loaded (2) after order is fetched (3) after shipment tracking is added
        verify(orderDetailRepository, times(3)).getOrderShipmentTrackings(any())
        assertThat(orderShipmentTrackings).isEqualTo(addedShipmentTrackings)
    }

    @Test
    fun `show shipping label creation if the order is eligible`() = testBlocking {
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())

        doReturn(Unit).whenever(orderDetailRepository).fetchSLCreationEligibility(order.id)
        doReturn(true).whenever(orderDetailRepository).isOrderEligibleForSLCreation(order.id)

        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
        doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
        doReturn(emptyList<Product>()).whenever(orderDetailRepository).fetchProductsByRemoteIds(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(shippingLabelOnboardingRepository).isShippingPluginReady

        var isCreateShippingLabelButtonVisible: Boolean? = null
        viewModel.viewStateData.observeForever { _, new ->
            isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
        }

        viewModel.start()

        assertThat(isCreateShippingLabelButtonVisible).isTrue()
    }

    @Test
    fun `hide shipping label creation if wcs is older than supported version`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(shippingLabelOnboardingRepository).isShippingPluginReady
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(emptyList<Product>()).whenever(orderDetailRepository).fetchProductsByRemoteIds(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            var isCreateShippingLabelButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
            }

            viewModel.start()

            verify(orderDetailRepository, never()).fetchSLCreationEligibility(any())
            verify(orderDetailRepository, never()).isOrderEligibleForSLCreation(any())
            assertThat(isCreateShippingLabelButtonVisible).isFalse()
        }

    @Test
    fun `hide shipping label creation if the order is not eligible`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(emptyList<Product>()).whenever(orderDetailRepository).fetchProductsByRemoteIds(any())

            var isCreateShippingLabelButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
            }

            viewModel.start()

            assertThat(isCreateShippingLabelButtonVisible).isFalse()
        }

    @Test
    fun `hide shipping label creation if wcs plugin is not installed`() =
        testBlocking {
            doReturn(false).whenever(shippingLabelOnboardingRepository).isShippingPluginReady
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(emptyList<Refund>()).whenever(orderDetailRepository).fetchOrderRefunds(any())
            doReturn(emptyList<Product>()).whenever(orderDetailRepository).fetchProductsByRemoteIds(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            var isCreateShippingLabelButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
            }

            viewModel.start()

            verify(orderDetailRepository, never()).fetchSLCreationEligibility(any())
            verify(orderDetailRepository, never()).isOrderEligibleForSLCreation(any())
            assertThat(isCreateShippingLabelButtonVisible).isFalse()
        }

    @Test
    fun `re-fetch order when payment flow completes`() = testBlocking {
        viewModel.start()
        val orderAfterPayment = order.copy(status = Order.Status.fromDataModel(CoreOrderStatus.COMPLETED)!!)
        doReturn(orderAfterPayment).whenever(orderDetailRepository).getOrderById(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

        viewModel.onCardReaderPaymentCompleted()

        assertThat(viewModel.order).isEqualTo(orderAfterPayment)
    }

    @Test
    fun `show order status updated snackbar on updating status from dialog`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            var snackbar: ShowUndoSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowUndoSnackbar) snackbar = it
            }

            viewModel.start()
            viewModel.onOrderStatusChanged(
                OrderStatusUpdateSource.Dialog(
                    oldStatus = order.status.value,
                    newStatus = CoreOrderStatus.PROCESSING.value
                )
            )

            assertThat(snackbar?.message).isEqualTo(resources.getString(string.order_status_updated))
        }

    @Test
    fun `show order status updated snackbar on updating status to completed from dialog`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            var snackbar: ShowUndoSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowUndoSnackbar) snackbar = it
            }

            viewModel.start()
            viewModel.onOrderStatusChanged(
                OrderStatusUpdateSource.Dialog(
                    oldStatus = order.status.value,
                    newStatus = CoreOrderStatus.COMPLETED.value
                )
            )

            assertThat(snackbar?.message).isEqualTo(resources.getString(string.order_status_updated))
        }

    @Test
    fun `show order completed snackbar on updating status to completed from fulfill screen`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            var snackbar: ShowUndoSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowUndoSnackbar) snackbar = it
            }

            viewModel.start()
            viewModel.onOrderStatusChanged(OrderStatusUpdateSource.FullFillScreen(order.status.value))

            assertThat(snackbar?.message).isEqualTo(resources.getString(string.order_fulfill_completed))
        }

    @Test
    fun `show error changing order snackbar if updating status failed`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            whenever(orderDetailRepository.updateOrderStatus(any(), any()))
                .thenReturn(
                    flow {
                        val event = OnOrderChanged(orderError = OrderError())
                        emit(UpdateOrderResult.RemoteUpdateResult(event))
                    }
                )
            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            viewModel.start()
            viewModel.onOrderStatusChanged(OrderStatusUpdateSource.FullFillScreen(order.status.value))

            assertThat(snackbar?.message).isEqualTo(string.order_error_update_general)
        }

    @Test
    fun `do not show error changing order snackbar if updating status failed because of cancellation`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(ContinuationWrapper.ContinuationResult.Cancellation<Boolean>(CancellationException())).whenever(
                orderDetailRepository
            ).updateOrderStatus(any(), any())
            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            viewModel.start()
            viewModel.onOrderStatusChanged(OrderStatusUpdateSource.FullFillScreen(order.status.value))

            assertThat(snackbar?.message).isNull()
        }

    @Test
    fun `do not show error changing order snackbar if updating status did not fail`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(ContinuationWrapper.ContinuationResult.Success(true)).whenever(orderDetailRepository)
                .updateOrderStatus(any(), any())
            var snackbar: ShowSnackbar? = null
            viewModel.event.observeForever {
                if (it is ShowSnackbar) snackbar = it
            }

            viewModel.start()
            viewModel.onOrderStatusChanged(OrderStatusUpdateSource.FullFillScreen(order.status.value))

            assertThat(snackbar?.message).isNull()
        }

    @Test
    fun `given receipt url available, when user taps on see receipt, then preview receipt screen shown`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            viewModel.onSeeReceiptClicked()

            assertThat(viewModel.event.value).isInstanceOf(PreviewReceipt::class.java)
        }

    @Test
    fun `when user taps on see receipt, then receipt view event is tracked`() =
        testBlocking {
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            viewModel.onSeeReceiptClicked()

            verify(orderDetailTracker).trackReceiptViewTapped(order.id, order.status)
        }

    @Test
    fun `given receipt request returns error, when user taps on see receipt, then snackbar event emitted`() =
        testBlocking {
            // GIVEN
            whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
            whenever(orderDetailRepository.fetchOrderNotes(any())).thenReturn(false)
            whenever(addonsRepository.containsAddonsFrom(any())).thenReturn(false)

            val errorMessage = "error"
            whenever(paymentReceiptHelper.getReceiptUrl(order.id)).thenReturn(Result.failure(Exception(errorMessage)))

            // WHEN
            viewModel.start()

            viewModel.onSeeReceiptClicked()

            // THEN
            assertThat((viewModel.event.value as ShowSnackbar).message).isEqualTo(string.receipt_fetching_error)
            verify(paymentsFlowTracker).trackReceiptUrlFetchingFails(
                errorDescription = errorMessage
            )
        }

    @Test
    fun `given receipt request returns success, when user taps on see receipt, then PreviewReceipt event emitted`() =
        testBlocking {
            // GIVEN
            whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
            whenever(orderDetailRepository.fetchOrderNotes(any())).thenReturn(false)
            whenever(addonsRepository.containsAddonsFrom(any())).thenReturn(false)
            val receiptUrl = "https://example.com"
            whenever(paymentReceiptHelper.getReceiptUrl(order.id)).thenReturn(Result.success(receiptUrl))

            // WHEN
            viewModel.start()

            viewModel.onSeeReceiptClicked()

            // THEN
            assertThat((viewModel.event.value as PreviewReceipt).orderId).isEqualTo(order.id)
            assertThat((viewModel.event.value as PreviewReceipt).receiptUrl).isEqualTo(receiptUrl)
            assertThat((viewModel.event.value as PreviewReceipt).billingEmail).isEqualTo(order.billingAddress.email)
        }

    @Test
    fun `when onSeeReceiptClicked clicked, then loading receipt status emitted`() =
        testBlocking {
            // GIVEN
            whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
            whenever(orderDetailRepository.fetchOrderNotes(any())).thenReturn(false)
            whenever(addonsRepository.containsAddonsFrom(any())).thenReturn(false)
            val receiptUrl = "https://example.com"
            whenever(paymentReceiptHelper.getReceiptUrl(order.id)).thenReturn(Result.success(receiptUrl))

            // WHEN
            viewModel.start()

            val states = viewModel.viewStateData.liveData.captureValues()

            viewModel.onSeeReceiptClicked()

            // THEN
            assertThat((states.last()).orderInfo!!.receiptButtonStatus).isEqualTo(
                OrderDetailViewState.ReceiptButtonStatus.Visible
            )
            assertThat((states[states.size - 2]).orderInfo!!.receiptButtonStatus).isEqualTo(
                OrderDetailViewState.ReceiptButtonStatus.Loading
            )
        }

    @Test
    fun `given order is paid, when status is processing order complete button should be visible`() =
        testBlocking {
            // Given
            val orderStatusStub = Order.Status.fromDataModel(CoreOrderStatus.PROCESSING)
            val orderStub = order.copy(datePaid = Date(), status = orderStatusStub!!)

            doReturn(orderStatus).whenever(orderDetailRepository).getOrderStatus(any())
            doReturn(orderStub).whenever(orderDetailRepository).getOrderById(any())

            var isMarkOrderCompleteButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isMarkOrderCompleteButtonVisible = new.isMarkOrderCompleteButtonVisible
            }

            // When
            viewModel.start()

            // Then
            assertThat(isMarkOrderCompleteButtonVisible).isTrue
        }

    @Test
    fun `given order is paid, when status is on hold order complete button should be visible`() =
        testBlocking {
            // Given
            val orderStatusStub = Order.Status.fromDataModel(CoreOrderStatus.ON_HOLD)
            val orderStub = order.copy(datePaid = Date(), status = orderStatusStub!!)

            doReturn(orderStatus).whenever(orderDetailRepository).getOrderStatus(any())
            doReturn(orderStub).whenever(orderDetailRepository).getOrderById(any())

            var isMarkOrderCompleteButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isMarkOrderCompleteButtonVisible = new.isMarkOrderCompleteButtonVisible
            }

            // When
            viewModel.start()

            // Then
            assertThat(isMarkOrderCompleteButtonVisible).isTrue
        }

    @Test
    fun `when order is not paid, order complete button should be hidden`() =
        testBlocking {
            // Given
            val orderStatusStub = Order.Status.fromDataModel(CoreOrderStatus.PROCESSING)
            val orderStub = order.copy(datePaid = null, status = orderStatusStub!!)

            doReturn(orderStatus).whenever(orderDetailRepository).getOrderStatus(any())
            doReturn(orderStub).whenever(orderDetailRepository).getOrderById(any())
            var isMarkOrderCompleteButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isMarkOrderCompleteButtonVisible = new.isMarkOrderCompleteButtonVisible
            }

            // When
            viewModel.start()

            // Then
            assertThat(isMarkOrderCompleteButtonVisible).isFalse
        }

    @Test
    fun `when order status complete, then hide mark order complete button`() =
        testBlocking {
            // Given
            val orderStatusStub = Order.Status.fromDataModel(CoreOrderStatus.COMPLETED)
            val orderStub = order.copy(datePaid = Date(), status = orderStatusStub!!)

            doReturn(orderStub).whenever(orderDetailRepository).getOrderById(any())
            doReturn(orderStatus.copy(statusKey = CoreOrderStatus.COMPLETED.value)).whenever(
                orderDetailRepository
            ).getOrderStatus(any())
            var isMarkOrderCompleteButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isMarkOrderCompleteButtonVisible = new.isMarkOrderCompleteButtonVisible
            }

            // When
            viewModel.start()

            // Then
            assertThat(isMarkOrderCompleteButtonVisible).isFalse()
        }

    @Test
    fun `given order is not paid, when status is on hold, order complete button should be hidden`() =
        testBlocking {
            // Given
            val orderStatusStub = Order.Status.fromDataModel(CoreOrderStatus.ON_HOLD)
            val orderStub = order.copy(datePaid = null, status = orderStatusStub!!)

            doReturn(orderStatus).whenever(orderDetailRepository).getOrderStatus(any())
            doReturn(orderStub).whenever(orderDetailRepository).getOrderById(any())

            var isMarkOrderCompleteButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isMarkOrderCompleteButtonVisible = new.isMarkOrderCompleteButtonVisible
            }

            // When
            viewModel.start()

            // Then
            assertThat(isMarkOrderCompleteButtonVisible).isFalse()
        }

    @Test
    fun `given order is not paid, when status is on failed, order complete button should be hidden`() =
        testBlocking {
            // Given
            val orderStatusStub = Order.Status.fromDataModel(CoreOrderStatus.FAILED)
            val orderStub = order.copy(datePaid = null, status = orderStatusStub!!)

            doReturn(orderStatus).whenever(orderDetailRepository).getOrderStatus(any())
            doReturn(orderStub).whenever(orderDetailRepository).getOrderById(any())

            var isMarkOrderCompleteButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isMarkOrderCompleteButtonVisible = new.isMarkOrderCompleteButtonVisible
            }

            // When
            viewModel.start()

            // Then
            assertThat(isMarkOrderCompleteButtonVisible).isFalse()
        }

    @Test
    fun `given order is not paid, when status is cacelled, order complete button should be hidden`() =
        testBlocking {
            // Given
            val orderStatusStub = Order.Status.fromDataModel(CoreOrderStatus.CANCELLED)
            val orderStub = order.copy(datePaid = null, status = orderStatusStub!!)

            doReturn(orderStatus).whenever(orderDetailRepository).getOrderStatus(any())
            doReturn(orderStub).whenever(orderDetailRepository).getOrderById(any())

            var isMarkOrderCompleteButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isMarkOrderCompleteButtonVisible = new.isMarkOrderCompleteButtonVisible
            }

            // When
            viewModel.start()

            // Then
            assertThat(isMarkOrderCompleteButtonVisible).isFalse()
        }

    @Test
    fun `given order is not paid, when status is pending, order complete button should be hidden`() =
        testBlocking {
            // Given
            val orderStatusStub = Order.Status.fromDataModel(CoreOrderStatus.PENDING)
            val orderStub = order.copy(datePaid = null, status = orderStatusStub!!)

            doReturn(orderStatus).whenever(orderDetailRepository).getOrderStatus(any())
            doReturn(orderStub).whenever(orderDetailRepository).getOrderById(any())

            var isMarkOrderCompleteButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isMarkOrderCompleteButtonVisible = new.isMarkOrderCompleteButtonVisible
            }

            // When
            viewModel.start()

            // Then
            assertThat(isMarkOrderCompleteButtonVisible).isFalse()
        }

    @Test
    fun `when user presses collect payment button, then start card reader payment flow`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            // When
            viewModel.onCollectPaymentClicked()

            // Then
            assertThat(viewModel.event.value).isInstanceOf(OrderNavigationTarget.StartPaymentFlow::class.java)
        }

    @Test
    fun `when user presses collect payment button, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            viewModel.start()

            // When
            viewModel.onCollectPaymentClicked()

            // Then
            verify(paymentsFlowTracker).trackCollectPaymentTapped(any())
        }

    @Test
    fun `when user refreshes order, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            viewModel.start()

            // When
            viewModel.onRefreshRequested()

            // Then
            verify(orderDetailTracker).trackOrderDetailPulledToRefresh()
        }

    @Test
    fun `when user adds a new shipment, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            // When
            viewModel.onNewShipmentTrackingAdded(testOrderShipmentTrackings[0])

            // Then
            verify(orderDetailTracker).trackAddOrderTrackingTapped(
                order.id,
                order.status,
                testOrderShipmentTrackings[0].trackingProvider
            )
        }

    @Test
    fun `when order status is changed, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            val updateSource = OrderStatusUpdateSource.Dialog(
                oldStatus = order.status.value,
                newStatus = CoreOrderStatus.PROCESSING.value
            )
            viewModel.start()

            // When
            viewModel.onOrderStatusChanged(updateSource)

            // Then
            verify(orderDetailTracker).trackOrderStatusChanged(
                order.id,
                order.status.value,
                updateSource.newStatus
            )
        }

    @Test
    fun `when user taps a create shipping label button, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            // When
            viewModel.onCreateShippingLabelButtonTapped()

            // Then
            verify(orderDetailTracker).trackShippinhLabelTapped()
        }

    @Test
    fun `when user taps a mark order complete button, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            // When
            viewModel.onMarkOrderCompleteButtonTapped()

            // Then
            verify(orderDetailTracker).trackMarkOrderAsCompleteTapped()
        }

    @Test
    fun `when user taps a view order addon button, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            // When
            viewModel.onViewOrderedAddonButtonTapped(order.items[0])

            // Then
            verify(orderDetailTracker).trackViewAddonsTapped()
        }

    @Test
    fun `when user taps order edit button, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            // When
            viewModel.onEditClicked()

            // Then
            verify(orderDetailTracker).trackEditButtonTapped(
                order.feesLines.size,
                order.shippingLines.size
            )
        }

    @Test
    fun `wait until all ongoing fetch request complete before fetching data again`() =
        testBlocking {
            // Given a work delay of 1s
            val mockWorkingDelay = 1_000L
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            whenever(orderDetailRepository.fetchOrderById(any())).doSuspendableAnswer {
                delay(mockWorkingDelay)
                order
            }
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())

            // When a fetch request is submitted while a fetch request is in progress
            viewModel.run {
                start()
                onRefreshRequested()
            }

            // Then verify data is fetched only once
            verify(orderDetailRepository, times(1)).fetchOrderById(any())
            verify(orderDetailRepository, times(1)).fetchOrderNotes(any())

            // Given the fetch request is completed
            advanceTimeBy(mockWorkingDelay + 1L)

            // When another fetch request is submitted
            viewModel.onRefreshRequested()

            // Then data is fetched again
            verify(orderDetailRepository, times(2)).fetchOrderById(any())
            verify(orderDetailRepository, times(2)).fetchOrderNotes(any())
        }

    @Test
    fun `when service plugin is installed and active, then fetch plugin data`() = testBlocking {
        doReturn(true).whenever(shippingLabelOnboardingRepository).isShippingPluginReady
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailRepository).fetchOrderShippingLabels(any())
        verify(orderDetailsTransactionLauncher).onShippingLabelFetchingCompleted()
    }

    @Test
    fun `when service plugin is NOT active, then DON'T fetch plugin data`() = testBlocking {
        doReturn(false).whenever(shippingLabelOnboardingRepository).isShippingPluginReady
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailRepository, never()).fetchOrderShippingLabels(any())
        verify(orderDetailsTransactionLauncher).onShippingLabelFetchingCompleted()
    }

    @Test
    fun `when service plugin is NOT installed, then DON'T fetch plugin data`() = testBlocking {
        doReturn(false).whenever(shippingLabelOnboardingRepository).isShippingPluginReady
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailRepository, never()).fetchOrderShippingLabels(any())
        verify(orderDetailsTransactionLauncher).onShippingLabelFetchingCompleted()
    }

    @Test
    fun `when shipment tracking plugin is installed and active, then fetch plugin data`() = testBlocking {
        val shipmentTracking = WooCommerceStore.WooPlugin.WOO_SHIPMENT_TRACKING.pluginName
        pluginsInfo[shipmentTracking] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailRepository).fetchOrderShipmentTrackingList(any())
        verify(orderDetailsTransactionLauncher).onShipmentTrackingFetchingCompleted()
    }

    @Test
    fun `when shipment tracking plugin is NOT active, then DON'T fetch plugin data`() = testBlocking {
        val shipmentTracking = WooCommerceStore.WooPlugin.WOO_SHIPMENT_TRACKING.pluginName
        pluginsInfo[shipmentTracking] = WooPlugin(
            isInstalled = true,
            isActive = false,
            version = "1.0.0"
        )
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailRepository, never()).fetchOrderShipmentTrackingList(any())
        verify(orderDetailsTransactionLauncher).onShipmentTrackingFetchingCompleted()
    }

    @Test
    fun `when shipment tracking plugin is NOT installed, then DON'T fetch plugin data`() = testBlocking {
        val shipmentTracking = WooCommerceStore.WooPlugin.WOO_SHIPMENT_TRACKING.pluginName
        pluginsInfo[shipmentTracking] = WooPlugin(
            isInstalled = false,
            isActive = false,
            version = null
        )
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailRepository, never()).fetchOrderShipmentTrackingList(any())
        verify(orderDetailsTransactionLauncher).onShipmentTrackingFetchingCompleted()
    }

    @Test
    fun `when there is no info about the plugins, then optimistically fetch plugin data`() = testBlocking {
        doReturn(true).whenever(shippingLabelOnboardingRepository).isShippingPluginReady
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())

        viewModel.start()

        verify(orderDetailRepository, times(1)).fetchOrderShippingLabels(any())
        verify(orderDetailRepository, times(1)).fetchOrderShipmentTrackingList(any())
    }

    @Test
    fun `when subscriptions plugin is installed and active, then fetch plugin data`() = testBlocking {
        val subscriptions = WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName
        pluginsInfo[subscriptions] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(emptyList<Subscription>()).whenever(getOrderSubscriptions).invoke(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(getOrderSubscriptions).invoke(any())
    }

    @Test
    fun `when subscriptions plugin is NOT active, then DON'T fetch plugin data`() = testBlocking {
        val subscriptions = WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName
        pluginsInfo[subscriptions] = WooPlugin(
            isInstalled = true,
            isActive = false,
            version = "1.0.0"
        )
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(getOrderSubscriptions, never()).invoke(any())
    }

    @Test
    fun `when subscriptions fetched is NOT empty, then track subscription shown event`() = testBlocking {
        val subscriptions = WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName
        pluginsInfo[subscriptions] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        val subscription: Subscription = mock()
        val result = listOf(subscription)

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(result).whenever(getOrderSubscriptions).invoke(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailTracker).trackOrderDetailsSubscriptionsShown()
    }

    @Test
    fun `when subscriptions fetched is empty, then DON'T track subscription shown event`() = testBlocking {
        val subscriptions = WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName
        pluginsInfo[subscriptions] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        val result = emptyList<Subscription>()

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(result).whenever(getOrderSubscriptions).invoke(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailTracker, never()).trackOrderDetailsSubscriptionsShown()
    }

    @Test
    fun `when subscriptions fetched is null, then DON'T track subscription shown event`() = testBlocking {
        val subscriptions = WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS.pluginName
        pluginsInfo[subscriptions] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        val result = null

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(result).whenever(getOrderSubscriptions).invoke(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailTracker, never()).trackOrderDetailsSubscriptionsShown()
    }

    @Test
    fun `when gift cards plugin is installed and active, then fetch plugin data`() = testBlocking {
        val giftCards = WooCommerceStore.WooPlugin.WOO_GIFT_CARDS.pluginName
        pluginsInfo[giftCards] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(giftCardRepository).fetchGiftCardSummaryByOrderId(any(), anyOrNull())
    }

    @Test
    fun `when gift cards plugin is NOT active, then DON'T fetch plugin data`() = testBlocking {
        val giftCards = WooCommerceStore.WooPlugin.WOO_GIFT_CARDS.pluginName
        pluginsInfo[giftCards] = WooPlugin(
            isInstalled = true,
            isActive = false,
            version = "1.0.0"
        )
        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(giftCardRepository, never()).fetchGiftCardSummaryByOrderId(any(), anyOrNull())
    }

    @Test
    fun `when gift cards fetched is NOT empty, then track gift cards shown event`() = testBlocking {
        val giftCards = WooCommerceStore.WooPlugin.WOO_GIFT_CARDS.pluginName
        pluginsInfo[giftCards] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        val giftCard: GiftCardSummary = mock()
        val result = WooResult(listOf(giftCard))

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(result).whenever(giftCardRepository).fetchGiftCardSummaryByOrderId(any(), anyOrNull())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailTracker).trackOrderDetailsGiftCardShown()
    }

    @Test
    fun `when gift cards fetched is empty, then DON'T track gift cards shown event`() = testBlocking {
        val giftCards = WooCommerceStore.WooPlugin.WOO_GIFT_CARDS.pluginName
        pluginsInfo[giftCards] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        val result = WooResult(emptyList<Subscription>())

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(result).whenever(giftCardRepository).fetchGiftCardSummaryByOrderId(any(), anyOrNull())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailTracker, never()).trackOrderDetailsGiftCardShown()
    }

    @Test
    fun `when gift cards fetched is null, then DON'T track gift cards shown event`() = testBlocking {
        val giftCards = WooCommerceStore.WooPlugin.WOO_GIFT_CARDS.pluginName
        pluginsInfo[giftCards] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        val result = WooResult(null)

        doReturn(order).whenever(orderDetailRepository).getOrderById(any())
        doReturn(result).whenever(giftCardRepository).fetchGiftCardSummaryByOrderId(any(), anyOrNull())
        doReturn(true).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailTracker, never()).trackOrderDetailsGiftCardShown()
    }

    @Test
    fun `when the order is null then don't fetch gift cards summaries`() = testBlocking {
        val giftCards = WooCommerceStore.WooPlugin.WOO_GIFT_CARDS.pluginName
        pluginsInfo[giftCards] = WooPlugin(
            isInstalled = true,
            isActive = true,
            version = "1.0.0"
        )
        doReturn(null).whenever(orderDetailRepository).getOrderById(any())
        doReturn(null).whenever(orderDetailRepository).fetchOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(giftCardRepository, never()).fetchGiftCardSummaryByOrderId(any(), anyOrNull())
    }

    @Test
    fun `when the order is opened then track order loaded with right types and add-ons`() = testBlocking {
        val items = OrderTestUtils.generateTestOrderItems(count = 2)
        val testOrder = order.copy(items = items)
        val types = "simple, bundle"
        val hasAddons = true
        doReturn(testOrder).whenever(orderDetailRepository).getOrderById(any())
        doReturn(types).whenever(orderDetailRepository).getUniqueProductTypes(any())
        doReturn(hasAddons).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(testOrder).whenever(orderDetailRepository).fetchOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailTracker).trackProductsLoaded(
            order.id,
            types,
            hasAddons
        )
    }

    @Test
    fun `when the order is opened then track order loaded with right types and add-ons 2`() = testBlocking {
        val items = OrderTestUtils.generateTestOrderItems(count = 2)
        val testOrder = order.copy(items = items)
        val types = "simple, variable"
        val hasAddons = false
        doReturn(testOrder).whenever(orderDetailRepository).getOrderById(any())
        doReturn(types).whenever(orderDetailRepository).getUniqueProductTypes(any())
        doReturn(hasAddons).whenever(addonsRepository).containsAddonsFrom(any())
        doReturn(testOrder).whenever(orderDetailRepository).fetchOrderById(any())
        doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
        createViewModel()

        viewModel.start()

        verify(orderDetailTracker).trackProductsLoaded(
            order.id,
            types,
            hasAddons
        )
    }

    @Test
    fun `given order ids passed, when ViewModel starts, then previous order navigations is not enabled`() =
        testBlocking {
            val newSavedState = OrderDetailFragmentArgs(
                orderId = ORDER_ID,
                allOrderIds = arrayOf(ORDER_ID, 2).toLongArray()
            ).toSavedStateHandle()

            createViewModel(newSavedState)

            viewModel.start()

            // Then
            assertThat(viewModel.previousOrderNavigationIsEnabled()).isFalse
        }

    @Test
    fun `when the order is the last one then it disables navigation to next order`() = testBlocking {
        val newSavedState = OrderDetailFragmentArgs(
            orderId = ORDER_ID,
            allOrderIds = arrayOf(2, ORDER_ID).toLongArray()
        ).toSavedStateHandle()

        createViewModel(newSavedState)

        viewModel.start()

        // Then
        assertThat(viewModel.nextOrderNavigationIsEnabled()).isFalse
    }

    @Test
    fun `when the order is in the middle then it enables navigation to previous and next order`() = testBlocking {
        val newSavedState = OrderDetailFragmentArgs(
            orderId = ORDER_ID,
            allOrderIds = arrayOf(2, ORDER_ID, 3).toLongArray()
        ).toSavedStateHandle()

        createViewModel(newSavedState)

        viewModel.start()

        assertThat(viewModel.previousOrderNavigationIsEnabled()).isTrue
        assertThat(viewModel.nextOrderNavigationIsEnabled()).isTrue
    }

    @Test
    fun `when the order is not in the list then it disables order navigation`() = testBlocking {
        val newSavedState = OrderDetailFragmentArgs(
            orderId = ORDER_ID,
            allOrderIds = arrayOf(2L, 3L).toLongArray()
        ).toSavedStateHandle()

        createViewModel(newSavedState)

        viewModel.start()

        assertThat(viewModel.orderNavigationIsEnabled()).isFalse
    }

    @Test
    fun `when there is only one order then it disables order navigation`() = testBlocking {
        val newSavedState = OrderDetailFragmentArgs(
            orderId = ORDER_ID,
            allOrderIds = arrayOf(ORDER_ID).toLongArray()
        ).toSavedStateHandle()

        createViewModel(newSavedState)

        viewModel.start()

        assertThat(viewModel.orderNavigationIsEnabled()).isFalse
    }

    @Test
    fun `when order attribution is loaded, then update state`() = testBlocking {
        val attribution = OrderAttributionInfo(
            sourceType = "referral",
            source = "Woo.com"
        )
        whenever(orderDetailRepository.getOrderById(any())).thenReturn(order)
        whenever(addonsRepository.containsAddonsFrom(any())).thenReturn(false)
        whenever(orderDetailRepository.getOrderAttributionInfo(ORDER_ID)).thenReturn(attribution)

        createViewModel()
        viewModel.start()

        val attributionState = viewModel.orderAttributionInfo.getOrAwaitValue()

        assertThat(attributionState).isEqualTo(attribution)
    }

    @Test
    fun `when trash button is clicked, then show an alert`() = testBlocking {
        createViewModel()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onTrashOrderClicked()
        }.last()

        assertThat(event).isInstanceOf(MultiLiveEvent.Event.ShowDialog::class.java)
    }

    @Test
    fun `when trash is confirmed, then communicate event to the parent fragment`() = testBlocking {
        createViewModel()

        val dialogEvent = viewModel.event.runAndCaptureValues {
            viewModel.onTrashOrderClicked()
        }.last() as MultiLiveEvent.Event.ShowDialog
        val event = viewModel.event.runAndCaptureValues {
            dialogEvent.positiveBtnAction?.onClick(mock(), 0)
        }.last()

        assertThat(event).isEqualTo(OrderDetailViewModel.TrashOrder(ORDER_ID))
        verify(analyticsTracker).track(AnalyticsEvent.ORDER_DETAIL_TRASH_TAPPED)
    }

    @Test
    fun `when we can get shipping titles from the cached shipping methods then refresh is NOT call`() =
        testBlocking {
            // Given
            val shippingMethod = ShippingMethod(id = "free_shipping", title = "Free Shipping")
            val orderShippingLines = listOf(
                Order.ShippingLine(
                    itemId = 1L,
                    methodTitle = "Free",
                    methodId = shippingMethod.id,
                    total = BigDecimal.ZERO,
                    totalTax = BigDecimal.ZERO
                ),
                Order.ShippingLine(
                    itemId = 2L,
                    methodTitle = "Another shipping",
                    methodId = "",
                    total = BigDecimal.TEN,
                    totalTax = BigDecimal.ZERO
                ),
            )
            val testOrder = order.copy(shippingLines = orderShippingLines)
            val shippingMethods = listOf(shippingMethod)

            doReturn(testOrder).whenever(orderDetailRepository).getOrderById(any())
            doReturn(testOrder).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(flowOf(shippingMethods)).whenever(getShippingMethodsWithOtherValue).invoke()

            createViewModel()

            viewModel.start()

            var shippingLineDetails: List<OrderDetailViewModel.ShippingLineDetails>? = null

            // When
            viewModel.viewStateData.observeForever { _, _ -> }
            viewModel.shippingLineList.observeForever { shippingLines ->
                shippingLineDetails = shippingLines
            }

            // Then
            assertThat(shippingLineDetails).isNotNull
            assertThat(shippingLineDetails?.size).isEqualTo(testOrder.shippingLines.size)

            val details = shippingLineDetails?.firstOrNull { it.shippingMethod?.title == shippingMethod.title }
            assertThat(details).isNotNull
            verify(refreshShippingMethods, never()).invoke()
        }

    @Test
    fun `when we can't get shipping titles from the cached shipping methods then refresh is call`() =
        testBlocking {
            // Given
            val orderShippingLines = listOf(
                Order.ShippingLine(
                    itemId = 1L,
                    methodTitle = "Free",
                    methodId = "free_shipping",
                    total = BigDecimal.ZERO,
                    totalTax = BigDecimal.ZERO
                ),
                Order.ShippingLine(
                    itemId = 2L,
                    methodTitle = "Another shipping",
                    methodId = "",
                    total = BigDecimal.TEN,
                    totalTax = BigDecimal.ZERO
                ),
            )
            val testOrder = order.copy(shippingLines = orderShippingLines)
            val shippingMethods = emptyList<ShippingMethod>()

            doReturn(testOrder).whenever(orderDetailRepository).getOrderById(any())
            doReturn(testOrder).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(flowOf(shippingMethods)).whenever(getShippingMethodsWithOtherValue).invoke()

            createViewModel()

            viewModel.start()

            var shippingLineDetails: List<OrderDetailViewModel.ShippingLineDetails>? = null

            // When
            viewModel.viewStateData.observeForever { _, _ -> }
            viewModel.shippingLineList.observeForever { shippingLines ->
                shippingLineDetails = shippingLines
            }

            // Then
            assertThat(shippingLineDetails).isNotNull
            assertThat(shippingLineDetails?.size).isEqualTo(testOrder.shippingLines.size)

            verify(refreshShippingMethods).invoke()
        }

    @Test
    fun `when we can't get shipping titles from the cached shipping methods then refresh is called one time`() =
        testBlocking {
            // Given
            val orderShippingLines = listOf(
                Order.ShippingLine(
                    itemId = 1L,
                    methodTitle = "Free",
                    methodId = "free_shipping",
                    total = BigDecimal.ZERO,
                    totalTax = BigDecimal.ZERO
                ),
                Order.ShippingLine(
                    itemId = 2L,
                    methodTitle = "Another shipping",
                    methodId = "",
                    total = BigDecimal.TEN,
                    totalTax = BigDecimal.ZERO
                ),
            )
            val testOrder = order.copy(shippingLines = orderShippingLines)
            val shippingMethods = emptyList<ShippingMethod>()

            doReturn(testOrder).whenever(orderDetailRepository).getOrderById(any())
            doReturn(testOrder).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(flowOf(shippingMethods, shippingMethods)).whenever(getShippingMethodsWithOtherValue).invoke()

            createViewModel()

            viewModel.start()

            var shippingLineDetails: List<OrderDetailViewModel.ShippingLineDetails>? = null

            // When
            viewModel.viewStateData.observeForever { _, _ -> }
            viewModel.shippingLineList.observeForever { shippingLines ->
                shippingLineDetails = shippingLines
            }

            // Then
            assertThat(shippingLineDetails).isNotNull
            assertThat(shippingLineDetails?.size).isEqualTo(testOrder.shippingLines.size)

            verify(refreshShippingMethods).invoke()
        }
}
