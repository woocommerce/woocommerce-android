package com.woocommerce.android.ui.orders

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillFragmentArgs
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillViewModel
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class OrderFullfillViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private const val ORDER_LOCAL_ID = 1
        private const val ORDER_SITE_ID = 1
    }

    private val networkStatus: NetworkStatus = mock()
    private val appPrefsWrapper: AppPrefs = mock {
        on(it.isTrackingExtensionAvailable()).thenAnswer { true }
    }
    private val selectedSite: SelectedSite = mock()
    private val repository: OrderDetailRepository = mock()
    private val resources: ResourceProvider = mock()

    private val savedState =
        OrderFulfillFragmentArgs(orderId = ORDER_ID, orderLocalId = ORDER_LOCAL_ID).initSavedStateHandle()

    private val order = OrderTestUtils.generateTestOrder(ORDER_ID)
    private val testOrderShipmentTrackings = OrderTestUtils.generateTestOrderShipmentTrackings(
        totalCount = 5,
        localOrderId = ORDER_LOCAL_ID,
        localSiteId = ORDER_SITE_ID,
    )
    private val orderShippingLabels = OrderTestUtils.generateShippingLabels(5)
    private val testOrderRefunds = OrderTestUtils.generateRefunds(1)
    private lateinit var viewModel: OrderFulfillViewModel

    private val orderWithParameters = ViewState(
        order = order,
        toolbarTitle = resources.getString(string.order_fulfill_title),
        isShipmentTrackingAvailable = true
    )

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()

        viewModel = spy(
            OrderFulfillViewModel(
                savedState,
                appPrefsWrapper,
                networkStatus,
                resources,
                repository
            )
        )

        clearInvocations(
            viewModel,
            selectedSite,
            repository,
            networkStatus,
            resources
        )
    }

    @Test
    fun `Displays the order detail view correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val nonRefundedOrder = order.copy(refundTotal = BigDecimal.ZERO)
        val expectedViewState = orderWithParameters.copy(order = order.copy(refundTotal = nonRefundedOrder.refundTotal))

        doReturn(nonRefundedOrder).whenever(repository).getOrderById(any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
        doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

        var orderData: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> orderData = new }

        // order shipment Trackings
        val shipmentTrackings = ArrayList<OrderShipmentTracking>()
        viewModel.shipmentTrackings.observeForever {
            it?.let {
                shipmentTrackings.clear()
                shipmentTrackings.addAll(it)
            }
        }

        // product list should not be empty when products are not refunded
        val products = ArrayList<Order.Item>()
        viewModel.productList.observeForever {
            it?.let { products.addAll(it) }
        }

        viewModel.start()

        assertThat(orderData).isEqualTo(expectedViewState)
        assertThat(shipmentTrackings).isNotEmpty
        assertThat(shipmentTrackings).isEqualTo(testOrderShipmentTrackings)
        assertThat(products).isNotEmpty
    }

    @Test
    fun `hasVirtualProductsOnly returns false if there are no products for the order`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = order.copy(items = emptyList())
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

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
            doReturn(virtualOrder).whenever(repository).getOrderById(any())

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
            doReturn(mixedOrder).whenever(repository).getOrderById(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            viewModel.start()

            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(false)
        }

    @Test
    fun `Do not display product list when all products are refunded`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
            doReturn(testOrderRefunds).whenever(repository).getOrderRefunds(any())

            // product list should not be empty when products are not refunded
            val products = ArrayList<Order.Item>()
            viewModel.productList.observeForever {
                it?.let { products.addAll(it) }
            }

            viewModel.start()

            assertThat(products).isEmpty()
        }

    @Test
    fun `Do not display shipment tracking when shipping labels are available`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(orderShippingLabels).whenever(repository).getOrderShippingLabels(any())

            var orderData: ViewState? = null
            viewModel.viewStateData.observeForever { _, new -> orderData = new }

            viewModel.start()
            assertThat(orderData?.isShipmentTrackingAvailable).isFalse()
        }

    @Test
    fun `Update order status when network connected`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(order).whenever(repository).getOrderById(any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

        var snackBar: ShowSnackbar? = null
        var exit: ExitWithResult<*>? = null
        viewModel.event.observeForever {
            if (it is ExitWithResult<*>) exit = it
            else if (it is ShowSnackbar) snackBar = it
        }

        viewModel.start()
        viewModel.onMarkOrderCompleteButtonClicked()

        assertThat(exit).isEqualTo(
            ExitWithResult(
                OrderDetailViewModel.OrderStatusUpdateSource.FullFillScreen(order.status.value),
                OrderFulfillViewModel.KEY_ORDER_FULFILL_RESULT
            )
        )
        assertNull(snackBar)
    }

    @Test
    fun `Do not update order status when not connected`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        var exit: ExitWithResult<*>? = null
        viewModel.event.observeForever {
            if (it is ExitWithResult<*>) exit = it
            else if (it is ShowSnackbar) snackbar = it
        }

        viewModel.order = order
        viewModel.onMarkOrderCompleteButtonClicked()

        assertNull(exit)
        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `refresh shipping tracking items when an item is added`() = runBlockingTest {
        val shipmentTracking = OrderShipmentTracking(
            trackingProvider = "testProvider",
            trackingNumber = "123456",
            dateShipped = DateUtils.getCurrentDateString()
        )

        doReturn(order).whenever(repository).getOrderById(any())

        val addedShipmentTrackings = testOrderShipmentTrackings.toMutableList()
        addedShipmentTrackings.add(shipmentTracking)
        doReturn(testOrderShipmentTrackings).doReturn(addedShipmentTrackings)
            .whenever(repository).getOrderShipmentTrackings(any())

        var orderShipmentTrackings = emptyList<OrderShipmentTracking>()
        viewModel.shipmentTrackings.observeForever {
            it?.let { orderShipmentTrackings = it }
        }

        viewModel.start()
        viewModel.onNewShipmentTrackingAdded(shipmentTracking)

        verify(repository, times(2)).getOrderShipmentTrackings(any())
        assertThat(orderShipmentTrackings).isEqualTo(addedShipmentTrackings)
    }

    @Test
    fun `handle onBackButtonClicked when new shipment tracking is added`() = runBlockingTest {
        val shipmentTracking = OrderShipmentTracking(
            trackingProvider = "testProvider",
            trackingNumber = "123456",
            dateShipped = DateUtils.getCurrentDateString()
        )

        doReturn(order).whenever(repository).getOrderById(any())

        val addedShipmentTrackings = testOrderShipmentTrackings.toMutableList()
        addedShipmentTrackings.add(shipmentTracking)
        doReturn(testOrderShipmentTrackings).doReturn(addedShipmentTrackings)
            .whenever(repository).getOrderShipmentTrackings(any())

        var orderShipmentTrackings = emptyList<OrderShipmentTracking>()
        viewModel.shipmentTrackings.observeForever {
            it?.let { orderShipmentTrackings = it }
        }

        var exit: Exit? = null
        var exitWithResult: ExitWithResult<*>? = null
        viewModel.event.observeForever {
            if (it is ExitWithResult<*>) exitWithResult = it
            else if (it is Exit) exit = it
        }

        viewModel.start()
        viewModel.onNewShipmentTrackingAdded(shipmentTracking)

        verify(repository, times(2)).getOrderShipmentTrackings(any())
        assertThat(orderShipmentTrackings).isEqualTo(addedShipmentTrackings)

        viewModel.onBackButtonClicked()
        assertNull(exit)
        assertThat(exitWithResult).isEqualTo(
            ExitWithResult(
                true, OrderFulfillViewModel.KEY_REFRESH_SHIPMENT_TRACKING_RESULT
            )
        )
    }

    @Test
    fun `handle onBackButtonClicked when no shipment is added`() = runBlockingTest {
        doReturn(order).whenever(repository).getOrderById(any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

        var exit: Exit? = null
        var exitWithResult: ExitWithResult<*>? = null
        viewModel.event.observeForever {
            if (it is ExitWithResult<*>) exitWithResult = it
            else if (it is Exit) exit = it
        }

        viewModel.start()
        viewModel.onBackButtonClicked()

        assertNotNull(exit)
        assertNull(exitWithResult)
    }
}
