package com.woocommerce.android.ui.woopos.orders.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.WooPosOrderActionsProvider
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersCoordinator
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderAction
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersUIEvent
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrderDetailsViewModelTest {

    @Rule @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val dataSource: WooPosOrdersDataSource = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val formatPrice: WooPosFormatPrice = mock()
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds = mock()
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker = mock()
    private val getProductById: WooPosGetProductById = mock()
    private val coordinator = WooPosOrdersCoordinator()
    private val networkStatus: WooPosNetworkStatus = mock()
    private lateinit var orderDetailsMapper: WooPosOrderDetailsMapper
    private lateinit var refundInfoBuilder: WooPosRefundInfoBuilder
    private lateinit var orderActionsProvider: WooPosOrderActionsProvider
    private lateinit var orderStatusMapper: WooPosOrderStatusMapper
    private lateinit var viewModel: WooPosOrderDetailsViewModel

    @Before
    fun setUp() = runTest {
        setupResourceProviderMocks()
        setupMockBehaviors()
        setupMappers()
        setupDataSourceMocks()
    }

    // region Loading via Coordinator

    @Test
    fun `when coordinator emits selectedOrderId, then state transitions to Loaded`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrderDetailsState.Loaded::class.java)
        val loaded = state as WooPosOrderDetailsState.Loaded
        assertThat(loaded.details.id).isEqualTo(1L)
        assertThat(loaded.dialogState).isEqualTo(WooPosOrderDetailsState.DialogState.Hidden)
    }

    @Test
    fun `when coordinator emits selectedOrderId, then analytics tracked`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // THEN
        verify(ordersAnalyticsTracker).trackOrderDetailsLoaded(
            orderId = eq(1L),
            orderStatus = any(),
            createdAtMillis = any()
        )
    }

    @Test
    fun `given order not found, when coordinator emits selectedOrderId, then state is Error`() = runTest {
        // GIVEN
        doReturn(Result.failure<Order>(RuntimeException("not found")))
            .whenever(dataSource).getOrderById(99L)
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        coordinator.selectOrder(99L)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrderDetailsState.Error::class.java)
    }

    @Test
    fun `when order loaded, then actions are available`() = runTest {
        // GIVEN
        val completedOrder = order(1).copy(status = Order.Status.Completed)
        doReturn(Result.success(completedOrder)).whenever(dataSource).getOrderById(1L)
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // THEN
        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.details.actions).isNotEmpty()
    }

    @Test
    fun `given loaded order, when coordinator emits null, then state transitions to Idle`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrderDetailsState.Loaded::class.java)

        // WHEN
        coordinator.selectOrder(null)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosOrderDetailsState.Idle)
    }

    // endregion

    // region Single Order Mode

    @Test
    fun `given single order mode, when init, then order is loaded automatically`() = runTest {
        // GIVEN
        doReturn(Result.success(order(42L))).whenever(dataSource).getOrderById(42L)
        val savedStateHandle = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to 42L))

        // WHEN
        viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.isSingleOrderMode).isTrue()
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrderDetailsState.Loaded::class.java)
        assertThat((state as WooPosOrderDetailsState.Loaded).details.id).isEqualTo(42L)
    }

    @Test
    fun `given single order mode with fetch failure, when init, then state is Error`() = runTest {
        // GIVEN
        doReturn(Result.failure<Order>(RuntimeException("not found")))
            .whenever(dataSource).getOrderById(42L)
        val savedStateHandle = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to 42L))

        // WHEN
        viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrderDetailsState.Error::class.java)
    }

    @Test
    fun `given no single order mode, when init, then state is Idle`() = runTest {
        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosOrderDetailsState.Idle)
        assertThat(viewModel.isSingleOrderMode).isFalse()
    }

    // endregion

    // region Actions

    @Test
    fun `given loaded order, when email receipt action clicked, then ToEmailReceipt event sent`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosOrdersUIEvent.OrderActionClicked(OrderAction.EmailReceipt(orderId = 1L)))
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(ToEmailReceipt(1L))
        verify(ordersAnalyticsTracker).trackOrderDetailsEmailReceiptTapped()
    }

    @Test
    fun `given completed order, when loaded, then IssueRefund action available`() = runTest {
        // GIVEN
        val completedOrder = order(1).copy(status = Order.Status.Completed)
        doReturn(Result.success(completedOrder)).whenever(dataSource).getOrderById(1L)
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // THEN
        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.details.actions).anyMatch { it is OrderAction.IssueRefund }
    }

    @Test
    fun `given pending order, when loaded, then no actions available`() = runTest {
        // GIVEN
        val pendingOrder = order(1).copy(status = Order.Status.Pending)
        doReturn(Result.success(pendingOrder)).whenever(dataSource).getOrderById(1L)
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // THEN
        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.details.actions).isEmpty()
    }

    @Test
    fun `given processing order, when loaded, then EmailReceipt action available`() = runTest {
        // GIVEN
        val processingOrder = order(1).copy(status = Order.Status.Processing)
        doReturn(Result.success(processingOrder)).whenever(dataSource).getOrderById(1L)
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // THEN
        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.details.actions).anyMatch { it is OrderAction.EmailReceipt }
    }

    // endregion

    // region Dialogs

    @Test
    fun `when back from issue refund, then order is refreshed`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // WHEN
        viewModel.onBackFromIssueRefund()
        advanceUntilIdle()

        // THEN
        verify(dataSource).refreshOrderById(1L)
    }

    @Test
    fun `given refunded order id, when back from issue refund after selection changed, then refunded order is refreshed`() =
        runTest {
            // GIVEN
            doReturn(Result.success(order(2L))).whenever(dataSource).getOrderById(2L)
            viewModel = createViewModel()
            advanceUntilIdle()
            coordinator.selectOrder(1L)
            advanceUntilIdle()
            coordinator.selectOrder(2L)
            advanceUntilIdle()

            // WHEN
            viewModel.onBackFromIssueRefund(orderId = 1L)
            advanceUntilIdle()

            // THEN
            verify(dataSource).refreshOrderById(1L)
            val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
            assertThat(loaded.details.id).isEqualTo(2L)
        }

    @Test
    fun `given selected order changed, when back from issue refund, then selected order state is untouched`() =
        runTest {
            // GIVEN
            val selectedOrder = order(2L).copy(number = "2002")
            val refreshedRefundedOrder = order(1L).copy(number = "1001")
            doReturn(Result.success(selectedOrder)).whenever(dataSource).getOrderById(2L)
            doReturn(Result.success(refreshedRefundedOrder)).whenever(dataSource).refreshOrderById(1L)
            viewModel = createViewModel()
            advanceUntilIdle()
            coordinator.selectOrder(1L)
            advanceUntilIdle()
            coordinator.selectOrder(2L)
            advanceUntilIdle()
            val selectedOrderDetails = (viewModel.state.value as WooPosOrderDetailsState.Loaded).details

            // WHEN
            viewModel.onBackFromIssueRefund(orderId = 1L)
            advanceUntilIdle()

            // THEN
            verify(dataSource).refreshOrderById(1L)
            val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
            assertThat(loaded.details).isEqualTo(selectedOrderDetails)
        }

    @Test
    fun `given state is not Loaded, when back from issue refund, then order is not refreshed`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onBackFromIssueRefund()
        advanceUntilIdle()

        // THEN
        verify(dataSource, never()).refreshOrderById(any())
    }

    @Test
    fun `given refund details dialog open, when dismissed, then dialog hidden`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // WHEN
        viewModel.onRefundDetailsDialogDismissed()
        advanceUntilIdle()

        // THEN
        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.dialogState).isEqualTo(WooPosOrderDetailsState.DialogState.Hidden)
    }

    // endregion

    // region Retry

    @Test
    fun `given error state, when retry called, then order reloaded`() = runTest {
        // GIVEN
        doReturn(Result.failure<Order>(RuntimeException("fail")))
            .whenever(dataSource).getOrderById(1L)
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrderDetailsState.Error::class.java)

        // WHEN
        doReturn(Result.success(order(1L))).whenever(dataSource).getOrderById(1L)
        viewModel.retryLoadOrder()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrderDetailsState.Loaded::class.java)
    }

    // endregion

    private fun order(id: Long = 1L): Order = OrderTestUtils.generateTestOrder(orderId = id).copy(
        datePaid = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z")
    )

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): WooPosOrderDetailsViewModel {
        return WooPosOrderDetailsViewModel(
            savedStateHandle = savedStateHandle,
            ordersDataSource = dataSource,
            resourceProvider = resourceProvider,
            childrenToParentEventSender = childrenToParentEventSender,
            retrieveOrderRefunds = retrieveOrderRefunds,
            ordersAnalyticsTracker = ordersAnalyticsTracker,
            orderDetailsMapper = orderDetailsMapper,
            refundInfoBuilder = refundInfoBuilder,
            formatPrice = formatPrice,
            coordinator = coordinator,
            networkStatus = networkStatus,
        )
    }

    private fun setupResourceProviderMocks() {
        whenever(resourceProvider.getString(R.string.date_time_connector)).thenReturn("at")
        whenever(resourceProvider.getString(R.string.woopos_orders_details_refund_error))
            .thenReturn("Refund error")
        whenever(resourceProvider.getString(R.string.woopos_orders_loading_error_message))
            .thenReturn("Please check your connection try again.")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_auto_draft)).thenReturn("Draft")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_pending)).thenReturn("Pending")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_processing)).thenReturn("Processing")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_on_hold)).thenReturn("On hold")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_failed)).thenReturn("Failed")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_cancelled)).thenReturn("Canceled")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_completed)).thenReturn("Completed")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_refunded)).thenReturn("Refunded")
        whenever(resourceProvider.getString(eq(R.string.woopos_orders_details_refund_label_numbered), any()))
            .thenAnswer { invocation ->
                val index = invocation.arguments[1] as Int
                "Refund #$index"
            }
        whenever(resourceProvider.getQuantityString(any(), any(), anyOrNull(), anyOrNull()))
            .thenAnswer { invocation ->
                val count = invocation.arguments[0] as Int
                if (count == 1) "Items subtotal ($count item)" else "Items subtotal ($count items)"
            }
    }

    private suspend fun setupMockBehaviors() {
        whenever(formatPrice(any<BigDecimal>(), any())).thenAnswer { invocation ->
            val amount = invocation.arguments[0] as? BigDecimal
            amount?.let { "$${it.abs()}" } ?: "$0.00"
        }
        whenever(formatPrice(any<BigDecimal>())).thenAnswer { invocation ->
            val amount = invocation.arguments[0] as? BigDecimal
            amount?.let { "$${it.abs()}" } ?: "$0.00"
        }
        whenever(getProductById.invoke(any())).thenReturn(null)
        whenever(retrieveOrderRefunds.invoke(any(), any())).thenReturn(Result.success(emptyList()))
    }

    private fun setupMappers() {
        val providedLocale = Locale.US
        orderStatusMapper = WooPosOrderStatusMapper(resourceProvider, providedLocale)
        refundInfoBuilder = WooPosRefundInfoBuilder(resourceProvider, formatPrice)
        orderActionsProvider = WooPosOrderActionsProvider()
        orderDetailsMapper = WooPosOrderDetailsMapper(
            resourceProvider,
            getProductById,
            formatPrice,
            orderStatusMapper,
            refundInfoBuilder,
            orderActionsProvider,
            WooPosGetNonRefundedItems(),
            WooPosGroupRefundedItems(),
        )
    }

    private suspend fun setupDataSourceMocks() {
        doReturn(Result.success(order(1L))).whenever(dataSource).getOrderById(any())
        doReturn(Result.success(order(1L))).whenever(dataSource).refreshOrderById(any())
        whenever(networkStatus.isConnected()).thenReturn(true)
    }

    // region Refresh

    @Test
    fun `when onBackFromSuccessfullySendingEmailReceipt, then order is refreshed`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // WHEN
        viewModel.onBackFromSuccessfullySendingEmailReceipt()
        advanceUntilIdle()

        // THEN
        verify(dataSource).refreshOrderById(1L)
    }

    @Test
    fun `given refresh in flight, when user selects different order, then coordinator notifies for original order`() =
        runTest {
            // GIVEN
            val refreshedIds = mutableListOf<Long>()
            val collectorJob = launch {
                coordinator.orderRefreshed.collect { refreshedIds.add(it) }
            }
            doReturn(Result.success(order(2L))).whenever(dataSource).getOrderById(2L)
            viewModel = createViewModel()
            advanceUntilIdle()
            coordinator.selectOrder(1L)
            advanceUntilIdle()

            // WHEN
            viewModel.onBackFromSuccessfullySendingEmailReceipt()
            coordinator.selectOrder(2L)
            advanceUntilIdle()

            // THEN
            assertThat(refreshedIds).contains(1L)
            val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
            assertThat(loaded.details.id).isEqualTo(2L)

            collectorJob.cancel()
        }

    @Test
    fun `given refresh fails, when back from issue refund, then refresh failed event emitted`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()
        val events = mutableListOf<Unit>()
        val collectorJob = launch { viewModel.refreshFailedEvent.collect { events.add(it) } }
        advanceUntilIdle()
        doReturn(Result.failure<Order>(RuntimeException("network error")))
            .whenever(dataSource).refreshOrderById(1L)

        // WHEN
        viewModel.onBackFromIssueRefund()
        advanceUntilIdle()

        // THEN
        assertThat(events).hasSize(1)

        collectorJob.cancel()
    }

    @Test
    fun `given no connection, when back from issue refund, then event emitted without fetching order`() = runTest {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(false)
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()
        val events = mutableListOf<Unit>()
        val collectorJob = launch { viewModel.refreshFailedEvent.collect { events.add(it) } }
        advanceUntilIdle()

        // WHEN
        viewModel.onBackFromIssueRefund()
        advanceUntilIdle()

        // THEN
        assertThat(events).hasSize(1)
        verify(dataSource, never()).refreshOrderById(any())

        collectorJob.cancel()
    }

    @Test
    fun `given refresh succeeds, when back from issue refund, then refresh failed event not emitted`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()
        val events = mutableListOf<Unit>()
        val collectorJob = launch { viewModel.refreshFailedEvent.collect { events.add(it) } }
        advanceUntilIdle()

        // WHEN
        viewModel.onBackFromSuccessfullySendingEmailReceipt()
        advanceUntilIdle()

        // THEN
        assertThat(events).isEmpty()

        collectorJob.cancel()
    }

    // endregion

    // region Retry

    @Test
    fun `given single order mode error, when retryLoadOrder called, then order reloaded`() = runTest {
        // GIVEN
        doReturn(Result.failure<Order>(RuntimeException("network error")))
            .whenever(dataSource).getOrderById(42L)
        val savedStateHandle = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to 42L))
        viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrderDetailsState.Error::class.java)

        // WHEN
        doReturn(Result.success(order(42L))).whenever(dataSource).getOrderById(42L)
        viewModel.retryLoadOrder()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrderDetailsState.Loaded::class.java)
    }

    @Test
    fun `given no lastRequestedOrderId, when retryLoadOrder called, then nothing happens`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.retryLoadOrder()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosOrderDetailsState.Idle)
    }

    // endregion
}
