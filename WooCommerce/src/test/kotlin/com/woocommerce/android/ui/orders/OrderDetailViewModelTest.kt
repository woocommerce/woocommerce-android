package com.woocommerce.android.ui.orders

import android.content.Context
import android.content.SharedPreferences
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
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
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PreviewReceipt
import com.woocommerce.android.ui.orders.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.OrderInfo
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.ViewState
import com.woocommerce.android.ui.prefs.cardreader.CardReaderTracker
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
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
    private val repository: OrderDetailRepository = mock()
    private val addonsRepository: AddonRepository = mock()
    private val cardReaderTracker: CardReaderTracker = mock()
    private val resources: ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
    }
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()

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
        isProductListMenuVisible = false
    )

    @Before
    fun setup() {
        doReturn(true).whenever(networkStatus).isConnected()

        doReturn(WooPlugin(true, true, version = OrderDetailViewModel.SUPPORTED_WCS_VERSION))
            .whenever(repository).getWooServicesPluginInfo()
        val site = SiteModel().let {
            it.id = 1
            it.siteId = 1
            it.selfHostedSiteId = 1
            it
        }
        doReturn(site).whenever(selectedSite).getIfExists()
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
                repository,
                addonsRepository,
                selectedSite,
                productImageMap,
                paymentCollectibilityChecker,
                cardReaderTracker,
            )
        )

        clearInvocations(
            viewModel,
            selectedSite,
            repository,
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

        val expectedViewState = orderWithParameters.copy(orderInfo = orderInfo.copy(order = nonRefundedOrder))

        doReturn(false).whenever(paymentCollectibilityChecker).isCollectable(any())

        doReturn(nonRefundedOrder).whenever(repository).getOrderById(any())

        doReturn(true).whenever(repository).fetchOrderNotes(any())
        doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

        doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

        doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(repository).fetchOrderNotes(any())
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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(repository).fetchOrderNotes(any())
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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

            viewModel.start()
            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(false)
        }

    @Test
    fun `hasVirtualProductsOnly returns true if and only if there are no physical products for the order`() =
        testBlocking {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val virtualItems = listOf(item.copy(productId = 3), item.copy(productId = 4))
            val virtualOrder = order.copy(items = virtualItems)

            doReturn(true).whenever(repository).hasVirtualProductsOnly(listOf(3, 4))
            doReturn(virtualOrder).whenever(repository).getOrderById(any())
            doReturn(virtualOrder).whenever(repository).fetchOrderById(any())

            doReturn(testOrderRefunds).whenever(repository).getOrderRefunds(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())

            viewModel.start()

            assertThat(viewModel.hasVirtualProductsOnly()).isEqualTo(true)
        }

    @Test
    fun `hasVirtualProductsOnly returns false if there are both virtual and physical products for the order`() =
        testBlocking {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val mixedItems = listOf(item, item.copy(productId = 2))
            val mixedOrder = order.copy(items = mixedItems)

            doReturn(false).whenever(repository).hasVirtualProductsOnly(listOf(1, 2))
            doReturn(mixedOrder).whenever(repository).getOrderById(any())
            doReturn(mixedOrder).whenever(repository).fetchOrderById(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
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
            doReturn(order).whenever(repository).getOrderById(any())

            viewModel.start()

            verify(repository, never()).fetchProductsByRemoteIds(ids)
        }

    @Test
    fun `fetch products if there are some missing`() =
        testBlocking {
            val item = OrderTestUtils.generateTestOrder().items.first().copy(productId = 1)
            val items = listOf(item, item.copy(productId = 2))
            val ids = items.map { it.productId }

            val order = order.copy(items = items)
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(1).whenever(repository).getProductCountForOrder(ids)

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            viewModel.start()

            verify(repository, atLeastOnce()).fetchProductsByRemoteIds(ids)
        }

    @Test
    fun `Do not display product list when all products are refunded`() =
        testBlocking {
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(testOrderRefunds).whenever(repository).fetchOrderRefunds(any())
            doReturn(testOrderRefunds).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
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

            assertThat(areProductsVisible).isFalse
            assertThat(refunds).isNotEmpty
        }

    @Test
    fun `Display product list when shipping labels are available`() =
        testBlocking {
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            doReturn(orderShippingLabels).whenever(repository).getOrderShippingLabels(any())
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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            doReturn(orderShippingLabels).whenever(repository).getOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            doReturn(
                WooPlugin(
                    isInstalled = true,
                    isActive = true,
                    version = OrderDetailViewModel.SUPPORTED_WCS_VERSION
                )
            ).whenever(repository).getWooServicesPluginInfo()
            doReturn(Unit).whenever(repository).fetchSLCreationEligibility(order.id)
            doReturn(true).whenever(repository).isOrderEligibleForSLCreation(order.id)

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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            doReturn(
                WooPlugin(
                    isInstalled = true,
                    isActive = true,
                    version = OrderDetailViewModel.SUPPORTED_WCS_VERSION
                )
            ).whenever(repository).getWooServicesPluginInfo()

            doReturn(Unit).whenever(repository).fetchSLCreationEligibility(order.id)
            doReturn(true).whenever(repository).isOrderEligibleForSLCreation(order.id)

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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            doReturn(orderShippingLabels).whenever(repository).getOrderShippingLabels(any())
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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(paymentCollectibilityChecker).isCollectable(any())

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

            doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            doReturn(
                WooPlugin(
                    isInstalled = true,
                    isActive = true,
                    version = OrderDetailViewModel.SUPPORTED_WCS_VERSION
                )
            ).whenever(repository).getWooServicesPluginInfo()

            doReturn(Unit).whenever(repository).fetchSLCreationEligibility(order.id)
            doReturn(true).whenever(repository).isOrderEligibleForSLCreation(order.id)

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
            // TODO malinajirka This test is failing but it seems like it's catching an actual bug.
            //  updateOrderState() is finished after displayProductAndShippingDetails(). Which means that
            //  displayProductAndShippingDetails is working with an outdated orderInfo and the UI is never updated...

            assertThat(shippingLabels).isEmpty()
            assertThat(isCreateShippingLabelButtonVisible).isFalse
            assertThat(isProductListMenuVisible).isFalse
        }

    @Test
    fun `Display error message on fetch order error`() = testBlocking {
        whenever(repository.fetchOrderById(ORDER_ID)).thenReturn(null)
        whenever(repository.getOrderById(ORDER_ID)).thenReturn(null)

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(repository, times(1)).fetchOrderById(ORDER_ID)

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.order_error_fetch_generic))
    }

    @Test
    fun `Shows and hides order detail skeleton correctly`() =
        testBlocking {
            doReturn(null).whenever(repository).getOrderById(any())

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
        doReturn(null).whenever(repository).getOrderById(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.start()

        verify(repository, times(1)).getOrderById(ORDER_ID)
        verify(repository, times(0)).fetchOrderById(any())

        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `Update order status and handle undo action`() = testBlocking {
        val newOrderStatus = OrderStatus(CoreOrderStatus.PROCESSING.value, CoreOrderStatus.PROCESSING.value)

        doReturn(order).whenever(repository).getOrderById(any())
        doReturn(order).whenever(repository).fetchOrderById(any())
        doReturn(orderStatus).doReturn(newOrderStatus).doReturn(orderStatus).whenever(repository).getOrderStatus(any())

        doReturn(true).whenever(repository).fetchOrderNotes(any())
        doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

        doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
        doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
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

        verify(repository, times(2)).updateOrderStatus(eq(order.id), statusChangeCaptor.capture())

        assertThat(listOf(initialStatus) + statusChangeCaptor.allValues).containsExactly(
            initialStatus, newStatus, initialStatus
        )
    }

    @Test
    fun `Update order status when network connected`() = testBlocking {
        val newOrderStatus = OrderStatus(CoreOrderStatus.PROCESSING.value, CoreOrderStatus.PROCESSING.value)

        doReturn(order).whenever(repository).getOrderById(any())
        doReturn(order).whenever(repository).fetchOrderById(any())
        doReturn(orderStatus).doReturn(newOrderStatus).whenever(repository).getOrderStatus(any())

        doReturn(true).whenever(repository).fetchOrderNotes(any())
        doReturn(testOrderNotes).whenever(repository).getOrderNotes(any())

        doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
        doReturn(emptyList<Refund>()).whenever(repository).getOrderRefunds(any())

        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any(),)
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
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
        doReturn(order).whenever(repository).getOrderById(any())
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

        verify(repository, never()).updateOrderStatus(any(), any())

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
        doReturn(order).whenever(repository).fetchOrderById(any())

        doReturn(true).whenever(repository).fetchOrderNotes(any())

        val addedShipmentTrackings = testOrderShipmentTrackings.toMutableList()
        addedShipmentTrackings.add(shipmentTracking)
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
        doReturn(testOrderShipmentTrackings).doReturn(addedShipmentTrackings)
            .whenever(repository).getOrderShipmentTrackings(any())

        doReturn(emptyList<ShippingLabel>()).whenever(repository).getOrderShippingLabels(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

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
    fun `show shipping label creation if the order is eligible`() = testBlocking {
        doReturn(order).whenever(repository).getOrderById(any())
        doReturn(order).whenever(repository).fetchOrderById(any())

        doReturn(WooPlugin(isInstalled = true, isActive = true, version = OrderDetailViewModel.SUPPORTED_WCS_VERSION))
            .whenever(repository).getWooServicesPluginInfo()
        doReturn(Unit).whenever(repository).fetchSLCreationEligibility(order.id)
        doReturn(true).whenever(repository).isOrderEligibleForSLCreation(order.id)

        doReturn(true).whenever(repository).fetchOrderNotes(any())
        doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
        doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
        doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
        doReturn(emptyList<Product>()).whenever(repository).fetchProductsByRemoteIds(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())

            doReturn(WooPlugin(isInstalled = true, isActive = true, version = "1.25.10")).whenever(repository)
                .getWooServicesPluginInfo()

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Product>()).whenever(repository).fetchProductsByRemoteIds(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            var isCreateShippingLabelButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
            }

            viewModel.start()

            verify(repository, never()).fetchSLCreationEligibility(any())
            verify(repository, never()).isOrderEligibleForSLCreation(any())
            assertThat(isCreateShippingLabelButtonVisible).isFalse()
        }

    @Test
    fun `hide shipping label creation if the order is not eligible`() =
        testBlocking {
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            doReturn(
                WooPlugin(
                    isInstalled = true,
                    isActive = true,
                    version = OrderDetailViewModel.SUPPORTED_WCS_VERSION
                )
            )
                .whenever(repository).getWooServicesPluginInfo()
            doReturn(Unit).whenever(repository).fetchSLCreationEligibility(order.id)
            doReturn(false).whenever(repository).isOrderEligibleForSLCreation(order.id)

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Product>()).whenever(repository).fetchProductsByRemoteIds(any())

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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())

            doReturn(
                WooPlugin(
                    isInstalled = false, isActive = false, version = OrderDetailViewModel.SUPPORTED_WCS_VERSION
                )
            ).whenever(repository).getWooServicesPluginInfo()

            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(RequestResult.SUCCESS).whenever(repository).fetchOrderShipmentTrackingList(any())
            doReturn(emptyList<ShippingLabel>()).whenever(repository).fetchOrderShippingLabels(any())
            doReturn(emptyList<Refund>()).whenever(repository).fetchOrderRefunds(any())
            doReturn(emptyList<Product>()).whenever(repository).fetchProductsByRemoteIds(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

            var isCreateShippingLabelButtonVisible: Boolean? = null
            viewModel.viewStateData.observeForever { _, new ->
                isCreateShippingLabelButtonVisible = new.isCreateShippingLabelButtonVisible
            }

            viewModel.start()

            verify(repository, never()).fetchSLCreationEligibility(any())
            verify(repository, never()).isOrderEligibleForSLCreation(any())
            assertThat(isCreateShippingLabelButtonVisible).isFalse()
        }

    @Test
    fun `re-fetch order when payment flow completes`() = testBlocking {
        viewModel.start()
        val orderAfterPayment = order.copy(status = Status.fromDataModel(CoreOrderStatus.COMPLETED)!!)
        doReturn(orderAfterPayment).whenever(repository).getOrderById(any())
        doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())

        viewModel.onCardReaderPaymentCompleted()

        assertThat(viewModel.order).isEqualTo(orderAfterPayment)
    }

    @Test
    fun `show order status updated snackbar on updating status from dialog`() =
        testBlocking {
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(repository).fetchOrderNotes(any())
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
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(repository).fetchOrderNotes(any())
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
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(repository).fetchOrderNotes(any())
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
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            whenever(repository.updateOrderStatus(any(), any()))
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
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(ContinuationWrapper.ContinuationResult.Cancellation<Boolean>(CancellationException())).whenever(
                repository
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
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(true).whenever(repository).fetchOrderNotes(any())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            doReturn(ContinuationWrapper.ContinuationResult.Success(true)).whenever(repository)
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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(false).whenever(repository).fetchOrderNotes(any())
            doReturn("testing url")
                .whenever(appPrefsWrapper).getReceiptUrl(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
            viewModel.start()

            viewModel.onSeeReceiptClicked()

            assertThat(viewModel.event.value).isInstanceOf(PreviewReceipt::class.java)
        }

    @Test
    fun `when user presses collect payment button, then start card reader payment flow`() =
        testBlocking {
            // Given
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())
            doReturn(false).whenever(repository).fetchOrderNotes(any())
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
            doReturn(order).whenever(repository).getOrderById(any())
            doReturn(order).whenever(repository).fetchOrderById(any())
            viewModel.start()

            // When
            viewModel.onAcceptCardPresentPaymentClicked()

            // Then
            verify(cardReaderTracker).trackCollectPaymentTapped()
        }
}
