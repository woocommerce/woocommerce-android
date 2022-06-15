package com.woocommerce.android.ui.orders

import android.content.Context
import android.content.SharedPreferences
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.Order.Status
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.CardReaderTracker
import com.woocommerce.android.ui.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PreviewReceipt
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.OrderInfo
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.ViewState
import com.woocommerce.android.ui.orders.details.ShippingLabelOnboardingRepository
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal
import java.util.concurrent.CancellationException

@ExperimentalCoroutinesApi
class OrderDetailViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private const val ORDER_SITE_ID = 1
    }

    private val networkStatus: NetworkStatus = mock()
    private val appPrefsWrapper: AppPrefs = mock {
        on(it.isTrackingExtensionAvailable()).thenAnswer { true }
    }
    private val editor = mock<SharedPreferences.Editor>()
    private val preferences = mock<SharedPreferences> { whenever(it.edit()).thenReturn(editor) }
    private val selectedSite: SelectedSite = mock()
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val addonsRepository: AddonRepository = mock()
    private val cardReaderTracker: CardReaderTracker = mock()
    private val analyticsTraWrapper: AnalyticsTrackerWrapper = mock()
    private val resources: ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
    }
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()
    private val shippingLabelOnboardingRepository: ShippingLabelOnboardingRepository = mock()

    private val savedState = OrderDetailFragmentArgs(orderId = ORDER_ID).initSavedStateHandle()

    private val productImageMap = mock<ProductImageMap>()

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

    private val orderWithParameters = ViewState(
        orderInfo = orderInfo,
        toolbarTitle = resources.getString(string.orderdetail_orderstatus_ordernum, order.number),
        isShipmentTrackingAvailable = true,
        isCreateShippingLabelButtonVisible = false,
        isProductListVisible = true,
        areShippingLabelsVisible = false,
        isProductListMenuVisible = false,
        isSharePaymentLinkVisible = false,
        wcShippingBannerVisible = false
    )

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
        doReturn(site).whenever(selectedSite).get()
        testBlocking {
            doReturn(false).whenever(paymentCollectibilityChecker).isCollectable(any())
        }

        viewModel = spy(
            OrderDetailViewModel(
                coroutinesTestRule.testDispatchers,
                savedState,
                appPrefsWrapper,
                networkStatus,
                resources,
                orderDetailRepository,
                addonsRepository,
                selectedSite,
                productImageMap,
                paymentCollectibilityChecker,
                cardReaderTracker,
                analyticsTraWrapper,
                shippingLabelOnboardingRepository
            )
        )

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

        doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
        doReturn(testOrderShipmentTrackings).whenever(orderDetailRepository).getOrderShipmentTrackings(any())

        doReturn(emptyList<Refund>()).whenever(orderDetailRepository).getOrderRefunds(any())

        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

        var orderData: ViewState? = null
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
        val products = ArrayList<Order.Item>()
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
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())

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

            var orderData: ViewState? = null
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

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(orderDetailRepository, times(1)).fetchOrderById(ORDER_ID)

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.order_error_fetch_generic))
    }

    @Test
    fun `Shows and hides order detail skeleton correctly`() =
        testBlocking {
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
            initialStatus, newStatus, initialStatus
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

        verify(orderDetailRepository, times(2)).getOrderShipmentTrackings(any())
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

            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
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
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(true).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(orderDetailRepository).fetchOrderShipmentTrackingList(any())
            doReturn(emptyList<ShippingLabel>()).whenever(orderDetailRepository).fetchOrderShippingLabels(any())
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
        val orderAfterPayment = order.copy(status = Status.fromDataModel(CoreOrderStatus.COMPLETED)!!)
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
            doReturn("testing url")
                .whenever(appPrefsWrapper).getReceiptUrl(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
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
            doReturn("testing url")
                .whenever(appPrefsWrapper).getReceiptUrl(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            viewModel.onSeeReceiptClicked()

            verify(analyticsTraWrapper).track(
                AnalyticsEvent.RECEIPT_VIEW_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_ORDER_ID to order.id,
                    AnalyticsTracker.KEY_STATUS to order.status
                )
            )
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
            viewModel.onAcceptCardPresentPaymentClicked()

            // Then
            assertThat(viewModel.event.value).isInstanceOf(OrderNavigationTarget.StartCardReaderPaymentFlow::class.java)
        }

    @Test
    fun `when user presses collect payment button, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            viewModel.start()

            // When
            viewModel.onAcceptCardPresentPaymentClicked()

            // Then
            verify(cardReaderTracker).trackCollectPaymentTapped()
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
            verify(analyticsTraWrapper).track(AnalyticsEvent.ORDER_DETAIL_PULLED_TO_REFRESH)
        }

    @Test
    fun `when user clicks on share payment url, then event tracked`() =
        testBlocking {
            // Given
            doReturn(order).whenever(orderDetailRepository).getOrderById(any())
            doReturn(order).whenever(orderDetailRepository).fetchOrderById(any())
            doReturn(false).whenever(orderDetailRepository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            // When
            viewModel.onSharePaymentUrlClicked()

            // Then
            verify(analyticsTraWrapper).track(AnalyticsEvent.ORDER_DETAIL_PAYMENT_LINK_SHARED)
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
            verify(analyticsTraWrapper).track(
                AnalyticsEvent.ORDER_TRACKING_ADD,
                mapOf(
                    AnalyticsTracker.KEY_ID to order.id,
                    AnalyticsTracker.KEY_STATUS to order.status,
                    AnalyticsTracker.KEY_CARRIER to testOrderShipmentTrackings[0].trackingProvider
                )
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
            verify(analyticsTraWrapper).track(
                AnalyticsEvent.ORDER_STATUS_CHANGE,
                mapOf(
                    AnalyticsTracker.KEY_ID to order.id,
                    AnalyticsTracker.KEY_FROM to order.status.value,
                    AnalyticsTracker.KEY_TO to updateSource.newStatus,
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_FLOW_EDITING
                )
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
            verify(analyticsTraWrapper).track(AnalyticsEvent.ORDER_DETAIL_CREATE_SHIPPING_LABEL_BUTTON_TAPPED)
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
            verify(analyticsTraWrapper).track(AnalyticsEvent.ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED)
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
            verify(analyticsTraWrapper).track(AnalyticsEvent.PRODUCT_ADDONS_ORDER_DETAIL_VIEW_PRODUCT_ADDONS_TAPPED)
        }
}
