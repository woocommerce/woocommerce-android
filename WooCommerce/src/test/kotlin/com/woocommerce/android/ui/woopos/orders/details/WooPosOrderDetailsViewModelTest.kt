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
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderActionsState
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
    private lateinit var orderDetailsMapper: WooPosOrderDetailsMapper
    private lateinit var refundInfoBuilder: WooPosRefundInfoBuilder
    private lateinit var orderActionsProvider: WooPosOrderActionsProvider
    private lateinit var orderStatusMapper: WooPosOrderStatusMapper
    private lateinit var viewModel: WooPosOrderDetailsViewModel
    private val providedLocale: Locale = Locale.US

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
        )
    }

    @Before
    fun setUp() = runTest {
        setupResourceProviderMocks()
        setupMockBehaviors()
        setupMappers()
        setupDataSourceMocks()
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
        // Use doReturn().whenever() pattern for suspend functions returning Result<T> (inline class)
        doReturn(Result.success(order(1L))).whenever(dataSource).getOrderById(any())
        doReturn(Result.success(order(1L))).whenever(dataSource).refreshOrderById(any())
    }

    // region loadOrder

    @Test
    fun `when loadOrder called, then state transitions to Loaded with details`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadOrder(1L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrderDetailsState.Loaded::class.java)
        val loaded = state as WooPosOrderDetailsState.Loaded
        assertThat(loaded.details.id).isEqualTo(1L)
        assertThat(loaded.dialogState).isEqualTo(WooPosOrderDetailsState.DialogState.Hidden)
    }

    @Test
    fun `when loadOrder called, then analytics tracked`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadOrder(1L)
        advanceUntilIdle()

        verify(ordersAnalyticsTracker).trackOrderDetailsLoaded(
            orderId = eq(1L),
            orderStatus = any(),
            createdAtMillis = any()
        )
    }

    @Test
    fun `given order not found, when loadOrder called, then state is Error`() = runTest {
        doReturn(Result.failure<Order>(RuntimeException("not found"))).whenever(dataSource).getOrderById(99L)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadOrder(99L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrderDetailsState.Error::class.java)
    }

    @Test
    fun `when loadOrder completes, then actions are side-loaded`() = runTest {
        val completedOrder = order(1).copy(status = Order.Status.Completed)
        doReturn(Result.success(completedOrder)).whenever(dataSource).getOrderById(1L)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadOrder(1L)
        advanceUntilIdle()

        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.details.actionsState).isInstanceOf(OrderActionsState.Loaded::class.java)
    }

    // endregion

    // region Single order mode

    @Test
    fun `given single order mode, when init, then order is loaded automatically`() = runTest {
        doReturn(Result.success(order(42L))).whenever(dataSource).getOrderById(42L)
        val savedStateHandle = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to 42L))

        viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        assertThat(viewModel.isSingleOrderMode).isTrue()
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrderDetailsState.Loaded::class.java)
        assertThat((state as WooPosOrderDetailsState.Loaded).details.id).isEqualTo(42L)
    }

    @Test
    fun `given single order mode with fetch failure, when init, then state is Error`() = runTest {
        doReturn(Result.failure<Order>(RuntimeException("not found"))).whenever(dataSource).getOrderById(42L)

        val savedStateHandle = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to 42L))
        viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isInstanceOf(WooPosOrderDetailsState.Error::class.java)
    }

    // endregion

    // region Email receipt

    @Test
    fun `when email receipt button clicked, then ToEmailReceipt event sent`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailReceiptButtonClicked(123L)
        advanceUntilIdle()

        verify(childrenToParentEventSender).sendToParent(ToEmailReceipt(123L))
        verify(ordersAnalyticsTracker).trackOrderDetailsEmailReceiptTapped()
    }

    // endregion

    // region Issue refund dialog

    @Test
    fun `given loaded state, when issue refund clicked, then dialog state is IssueRefund`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadOrder(1L)
        advanceUntilIdle()

        viewModel.onIssueRefundButtonClicked(1L)

        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.dialogState).isInstanceOf(WooPosOrderDetailsState.DialogState.IssueRefund::class.java)
        assertThat((loaded.dialogState as WooPosOrderDetailsState.DialogState.IssueRefund).orderId).isEqualTo(1L)
    }

    @Test
    fun `given issue refund dialog open, when dismissed, then dialog hidden`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadOrder(1L)
        advanceUntilIdle()
        viewModel.onIssueRefundButtonClicked(1L)

        viewModel.onIssueRefundDialogDismissed()
        advanceUntilIdle()

        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.dialogState).isEqualTo(WooPosOrderDetailsState.DialogState.Hidden)
    }

    // endregion

    // region Refund details dialog

    @Test
    fun `given refund details dialog open, when dismissed, then dialog hidden`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadOrder(1L)
        advanceUntilIdle()

        viewModel.onRefundDetailsDialogDismissed()
        advanceUntilIdle()

        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        assertThat(loaded.dialogState).isEqualTo(WooPosOrderDetailsState.DialogState.Hidden)
    }

    // endregion

    // region Order actions by status

    @Test
    fun `given completed order, when loaded, then IssueRefund action available`() = runTest {
        val completedOrder = order(1).copy(status = Order.Status.Completed)
        doReturn(Result.success(completedOrder)).whenever(dataSource).getOrderById(1L)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadOrder(1L)
        advanceUntilIdle()

        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        val actions = loaded.details.actionsState as OrderActionsState.Loaded
        assertThat(actions.actions).anyMatch {
            it is com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderAction.IssueRefund
        }
    }

    @Test
    fun `given pending order, when loaded, then no actions available`() = runTest {
        val pendingOrder = order(1).copy(status = Order.Status.Pending)
        doReturn(Result.success(pendingOrder)).whenever(dataSource).getOrderById(1L)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadOrder(1L)
        advanceUntilIdle()

        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        val actions = loaded.details.actionsState as OrderActionsState.Loaded
        assertThat(actions.actions).isEmpty()
    }

    @Test
    fun `given processing order, when loaded, then EmailReceipt action available`() = runTest {
        val processingOrder = order(1).copy(status = Order.Status.Processing)
        doReturn(Result.success(processingOrder)).whenever(dataSource).getOrderById(1L)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadOrder(1L)
        advanceUntilIdle()

        val loaded = viewModel.state.value as WooPosOrderDetailsState.Loaded
        val actions = loaded.details.actionsState as OrderActionsState.Loaded
        assertThat(actions.actions).anyMatch {
            it is com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderAction.EmailReceipt
        }
    }

    // endregion

    // region Idle state

    @Test
    fun `given no single order mode, when init, then state is Idle`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(WooPosOrderDetailsState.Idle)
        assertThat(viewModel.isSingleOrderMode).isFalse()
    }

    // endregion
}
