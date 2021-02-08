package com.woocommerce.android.ui.orders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.ViewState
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class OrderDetailViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_IDENTIFIER = "1-1-1"
    }

    private val networkStatus: NetworkStatus = mock()
    private val appPrefsWrapper: AppPrefs = mock {
        on(it.isTrackingExtensionAvailable()).thenAnswer { true }
    }
    private val selectedSite: SelectedSite = mock()
    private val repository: OrderDetailRepository = mock()
    private val resources: ResourceProvider = mock {
        on(it.getString(any(), any())).thenAnswer { i -> i.arguments[0].toString() }
    }

    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            OrderDetailFragmentArgs(orderId = ORDER_IDENTIFIER)
        )
    )

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    private val order = OrderTestUtils.generateTestOrder(ORDER_IDENTIFIER)
    private val orderStatus = OrderStatus(order.status.value, order.status.value)
    private val testOrderNotes = OrderTestUtils.generateTestOrderNotes(5, ORDER_IDENTIFIER)
    private val testOrderShipmentTrackings = OrderTestUtils.generateTestOrderShipmentTrackings(5, ORDER_IDENTIFIER)
    private val orderShippingLabels = OrderTestUtils.generateShippingLabels(5, ORDER_IDENTIFIER)
    private val testOrderRefunds = OrderTestUtils.generateRefunds(1)
    private lateinit var viewModel: OrderDetailViewModel

    private val orderWithParameters = ViewState(
        order = order,
        isRefreshing = false,
        isOrderDetailSkeletonShown = false,
        toolbarTitle = resources.getString(string.orderdetail_orderstatus_ordernum, order.number),
        isShipmentTrackingAvailable = true,
        isCreateShippingLabelButtonVisible = false,
        isProductListVisible = true,
        areShippingLabelsVisible = false
    )

    private val mixedProducts = listOf(
        ProductTestUtils.generateProduct(1),
        ProductTestUtils.generateProduct(2, true)
    ).map { it.toDataModel(null) }

    private val virtualProducts = listOf(
        ProductTestUtils.generateProduct(3, true),
        ProductTestUtils.generateProduct(4, true)
    ).map { it.toDataModel(null) }

    @Before
    fun setup() {
        doReturn(MutableLiveData(ViewState()))
            .whenever(savedState).getLiveData<ViewState>(any(), any())
        doReturn(true).whenever(networkStatus).isConnected()

        doReturn(WooPlugin(true, true)).whenever(repository).getWooServicesPluginInfo()

        viewModel = spy(OrderDetailViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            appPrefsWrapper,
            networkStatus,
            resources,
            repository
        ))

        clearInvocations(
            viewModel,
            savedState,
            selectedSite,
            repository,
            networkStatus,
            resources
        )
    }

    @Test
    fun `Displays the order detail view correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val nonRefundedOrder = order.copy(refundTotal = BigDecimal.ZERO)
        val expectedViewState = orderWithParameters.copy(order = nonRefundedOrder)

        doReturn(nonRefundedOrder).whenever(repository).getOrder(any())

        doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
        doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

        doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

        doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

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
    fun `hasVirtualProductsOnly returns false if there are no products for the order`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = order.copy(items = emptyList())
            doReturn(order).whenever(repository).getOrder(any())
            doReturn(order).whenever(repository).fetchOrder(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

            viewModel.start()
            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(false)
        }

    @Test
    fun `hasVirtualProductsOnly returns true if and only if there are no physical products for the order`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val virtualItems = listOf(item.copy(productId = 3), item.copy(productId = 4))
            val virtualOrder = order.copy(items = virtualItems)

            doReturn(true).whenever(repository).hasVirtualProductsOnly(listOf(3, 4))
            doReturn(virtualOrder).whenever(repository).getOrder(any())
            doReturn(virtualOrder).whenever(repository).fetchOrder(any())

            doReturn(testOrderRefunds).whenever(repository).getOrderRefunds(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

            viewModel.start()

            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(true)
        }

    @Test
    fun `hasVirtualProductsOnly returns false if there are both virtual and physical products for the order`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val mixedItems = listOf(item, item.copy(productId = 2))
            val mixedOrder = order.copy(items = mixedItems)

            doReturn(false).whenever(repository).hasVirtualProductsOnly(listOf(1, 2))
            doReturn(mixedOrder).whenever(repository).getOrder(any())
            doReturn(mixedOrder).whenever(repository).fetchOrder(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

            viewModel.start()

            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(false)
        }

    @Test
    fun `don't fetch products if we have all products`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val items = listOf(item, item.copy(productId = 2))
            val ids = items.map { it.productId }

            val order = order.copy(items = items)
            doReturn(order).whenever(repository).getOrder(any())

            viewModel.start()

            verify(repository, never()).fetchProductsByRemoteIds(ids)
        }

    @Test
    fun `fetch products if there are some missing`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val items = listOf(item, item.copy(productId = 2))
            val ids = items.map { it.productId }

            val order = order.copy(items = items)
            doReturn(order).whenever(repository).getOrder(any())
            doReturn(order).whenever(repository).fetchOrder(any())
            doReturn(1).whenever(repository).getProductCountForOrder(ids)

            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

            viewModel.start()

            verify(repository, atLeastOnce()).fetchProductsByRemoteIds(ids)
        }

    @Test
    fun `Do not display product list when all products are refunded`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(order).whenever(repository).getOrder(any())
            doReturn(order).whenever(repository).fetchOrder(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(testOrderRefunds).whenever(repository).fetchOrderRefunds(any())
            doReturn(testOrderRefunds).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

            val refunds = ArrayList<Refund>()
            viewModel.orderRefunds.observeForever {
                it?.let { refunds.addAll(it) }
            }

            var areProductsVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                areProductsVisible = new.isProductListVisible
            }

            viewModel.start()

            assertThat(areProductsVisible).isFalse()
            assertThat(refunds).isNotEmpty
        }

    @Test
    fun `Do not display product list when shipping labels are available`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(order).whenever(repository).getOrder(any())
            doReturn(order).whenever(repository).fetchOrder(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            doReturn(orderShippingLabels).whenever(repository).getOrderShippingLabels(any())

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
            assertThat(areProductsVisible).isFalse()
        }

    @Test
    fun `Do not display shipment tracking when shipping labels are available`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(order).whenever(repository).getOrder(any())
            doReturn(order).whenever(repository).fetchOrder(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            doReturn(orderShippingLabels).whenever(repository).getOrderShippingLabels(any())

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
    fun `Display error message on fetch order error`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        whenever(repository.fetchOrder(ORDER_IDENTIFIER)).thenReturn(null)
        whenever(repository.getOrder(ORDER_IDENTIFIER)).thenReturn(null)

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(repository, times(1)).fetchOrder(ORDER_IDENTIFIER)

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.order_error_fetch_generic))
    }

    @Test
    fun `Shows and hides order detail skeleton correctly`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(null).whenever(repository).getOrder(any())

            val isSkeletonShown = ArrayList<Boolean>()
            viewModel.viewStateData.observeForever { old, new ->
                new.isOrderDetailSkeletonShown?.takeIfNotEqualTo(old?.isOrderDetailSkeletonShown) {
                    isSkeletonShown.add(it)
                }
            }

            viewModel.start()
            assertThat(isSkeletonShown).containsExactly(false, true, false)
        }

    @Test
    fun `Do not fetch order from api when not connected`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(null).whenever(repository).getOrder(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(repository, times(1)).getOrder(ORDER_IDENTIFIER)
        verify(repository, times(0)).fetchOrder(any())

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `Update order status and handle undo action`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val newOrderStatus = OrderStatus(CoreOrderStatus.PROCESSING.value, CoreOrderStatus.PROCESSING.value)

        doReturn(order).whenever(repository).getOrder(any())
        doReturn(order).whenever(repository).fetchOrder(any())
        doReturn(orderStatus).doReturn(newOrderStatus).doReturn(orderStatus).whenever(repository).getOrderStatus(any())

        doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
        doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

        doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
        doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

        var snackbar: ShowUndoSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowUndoSnackbar) snackbar = it
        }

        val orderStatusList = ArrayList<OrderStatus>()
        viewModel.viewStateData.observeForever { old, new ->
            new.orderStatus?.takeIfNotEqualTo(old?.orderStatus) { orderStatusList.add(it) }
        }

        viewModel.start()

        val oldStatus = order.status
        val newStatus = CoreOrderStatus.PROCESSING.value
        viewModel.onOrderStatusChanged(newStatus)

        assertThat(snackbar?.message).isEqualTo(resources.getString(string.order_status_changed_to, newStatus))

        // simulate undo click event
        viewModel.onOrderStatusChangeReverted()
        assertThat(orderStatusList).containsExactly(
            OrderStatus(statusKey = oldStatus.value, label = oldStatus.value),
            OrderStatus(statusKey = newStatus, label = newStatus),
            OrderStatus(statusKey = oldStatus.value, label = oldStatus.value)
        )
    }

    @Test
    fun `Update order status when network connected`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val newOrderStatus = OrderStatus(CoreOrderStatus.PROCESSING.value, CoreOrderStatus.PROCESSING.value)

        doReturn(order).whenever(repository).getOrder(any())
        doReturn(order).whenever(repository).fetchOrder(any())
        doReturn(orderStatus).doReturn(newOrderStatus).whenever(repository).getOrderStatus(any())

        doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
        doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

        doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
        doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

        var newOrder: Order? = null
        viewModel.viewStateData.observeForever { old, new ->
            new.order?.takeIfNotEqualTo(old?.order) { newOrder = it }
        }

        viewModel.start()
        viewModel.onOrderStatusChanged(CoreOrderStatus.PROCESSING.value)

        assertThat(newOrder?.status).isEqualTo(order.status)
    }

    @Test
    fun `Do not update order status when not connected`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(order).whenever(repository).getOrder(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()
        viewModel.onOrderStatusChanged(CoreOrderStatus.PROCESSING.value)
        viewModel.updateOrderStatus(CoreOrderStatus.PROCESSING.value)

        verify(repository, times(0)).updateOrderStatus(any(), any(), any())

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `refresh shipping tracking items when an item is added`() = runBlockingTest {
        val shipmentTracking = OrderShipmentTracking(
            trackingProvider = "testProvider",
            trackingNumber = "123456",
            dateShipped = DateUtils.getCurrentDateString()
        )

        doReturn(order).whenever(repository).getOrder(any())
        doReturn(order).whenever(repository).fetchOrder(any())

        doReturn(true).whenever(repository).fetchOrderNotes(any(), any())

        val addedShipmentTrackings = testOrderShipmentTrackings.toMutableList()
        addedShipmentTrackings.add(shipmentTracking)
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(), any())
        doReturn(testOrderShipmentTrackings).doReturn(addedShipmentTrackings)
            .whenever(repository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

        var orderShipmentTrackings = emptyList<OrderShipmentTracking>()
        viewModel.shipmentTrackings.observeForever {
            it?.let { orderShipmentTrackings = it }
        }

        viewModel.start()
        viewModel.onNewShipmentTrackingAdded(shipmentTracking)

        verify(repository, times(2)).getOrderShipmentTrackings(any())
        assertThat(orderShipmentTrackings).isEqualTo(addedShipmentTrackings)
    }
}
