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
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderActionsState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersUIEvent
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val bookingInfoMapper: WooPosBookingInfoMapper = mock()
    private val getProductById: WooPosGetProductById = mock()
    private val coordinator = WooPosOrdersCoordinator()
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
    fun `when order loaded, then actions are side-loaded`() = runTest {
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
        assertThat(loaded.details.actionsState).isInstanceOf(OrderActionsState.Loaded::class.java)
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
    fun `given loaded order, when issue refund action clicked, then dialog state is IssueRefund`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosOrdersUIEvent.OrderActionClicked(OrderAction.IssueRefund(orderId = 1L)))

        // THEN
        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.dialogState)
            .isInstanceOf(WooPosOrderDetailsState.DialogState.IssueRefund::class.java)
        assertThat(
            (loaded.dialogState as WooPosOrderDetailsState.DialogState.IssueRefund).orderId
        ).isEqualTo(1L)
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
        val actions = loaded.details.actionsState as OrderActionsState.Loaded
        assertThat(actions.actions).anyMatch { it is OrderAction.IssueRefund }
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
        val actions = loaded.details.actionsState as OrderActionsState.Loaded
        assertThat(actions.actions).isEmpty()
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
        val actions = loaded.details.actionsState as OrderActionsState.Loaded
        assertThat(actions.actions).anyMatch { it is OrderAction.EmailReceipt }
    }

    // endregion

    // region Dialogs

    @Test
    fun `given issue refund dialog open, when dismissed, then dialog hidden`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()
        viewModel.onUIEvent(WooPosOrdersUIEvent.OrderActionClicked(OrderAction.IssueRefund(orderId = 1L)))

        // WHEN
        viewModel.onIssueRefundDialogDismissed()
        advanceUntilIdle()

        // THEN
        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.dialogState).isEqualTo(WooPosOrderDetailsState.DialogState.Hidden)
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
            orderActionsProvider = orderActionsProvider,
            bookingInfoMapper = bookingInfoMapper,
            formatPrice = formatPrice,
            coordinator = coordinator,
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
            bookingInfoMapper,
            WooPosGetNonRefundedItems(),
            WooPosGroupRefundedItems(),
        )
    }

    private suspend fun setupDataSourceMocks() {
        doReturn(Result.success(order(1L))).whenever(dataSource).getOrderById(any())
        doReturn(Result.success(order(1L))).whenever(dataSource).refreshOrderById(any())
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
    fun `given issue refund dialog open, when dismissed, then order is refreshed`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        coordinator.selectOrder(1L)
        advanceUntilIdle()
        viewModel.onUIEvent(WooPosOrdersUIEvent.OrderActionClicked(OrderAction.IssueRefund(1L)))

        // WHEN
        viewModel.onIssueRefundDialogDismissed()
        advanceUntilIdle()

        // THEN
        verify(dataSource).refreshOrderById(1L)
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
