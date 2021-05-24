package com.woocommerce.android.ui.orders

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R.string
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillFragmentArgs
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillRepository
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillViewModel
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class OrderFullfillViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_IDENTIFIER = "1-1-1"
    }

    private val networkStatus: NetworkStatus = mock()
    private val appPrefsWrapper: AppPrefs = mock {
        on(it.isTrackingExtensionAvailable()).thenAnswer { true }
    }
    private val selectedSite: SelectedSite = mock()
    private val repository: OrderFulfillRepository = mock()
    private val resources: ResourceProvider = mock {
        on(it.getString(any(), any())).thenAnswer { i -> i.arguments[0].toString() }
    }

    private val savedState = OrderFulfillFragmentArgs(orderIdentifier = ORDER_IDENTIFIER).initSavedStateHandle()

    private val order = OrderTestUtils.generateTestOrder(ORDER_IDENTIFIER)
    private val testOrderShipmentTrackings = OrderTestUtils.generateTestOrderShipmentTrackings(5, ORDER_IDENTIFIER)
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

        doReturn(nonRefundedOrder).whenever(repository).getOrder(any())
        doReturn(testOrderShipmentTrackings).whenever(repository).getOrderShipmentTrackings(any())
        doReturn(nonRefundedOrder.items).whenever(repository).getNonRefundedProducts(any(), any())

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
}
