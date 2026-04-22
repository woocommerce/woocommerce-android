package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundsState
import com.woocommerce.android.ui.woopos.orders.details.WooPosBookingInfoMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosGetNonRefundedItems
import com.woocommerce.android.ui.woopos.orders.details.WooPosGroupRefundedItems
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderStatusMapper
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrdersViewModelTest {

    @Rule @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val dataSource: WooPosOrdersDataSource = mock()
    private lateinit var viewModel: WooPosOrdersViewModel

    private val resourceProvider: ResourceProvider = mock()
    private val getProductById: WooPosGetProductById = mock()
    private val formatPrice: WooPosFormatPrice = mock()
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds = mock()
    private val providedLocale: Locale = Locale.US
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker = mock()
    private val bookingInfoMapper: WooPosBookingInfoMapper = mock()
    private lateinit var orderItemMapper: WooPosOrderItemMapper
    private lateinit var orderDetailsMapper: WooPosOrderDetailsMapper
    private lateinit var refundInfoBuilder: WooPosRefundInfoBuilder
    private lateinit var orderActionsProvider: WooPosOrderActionsProvider
    private lateinit var orderStatusMapper: WooPosOrderStatusMapper

    private fun order(id: Long = 1L): Order = OrderTestUtils.generateTestOrder(orderId = id).copy(
        datePaid = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z")
    )

    private fun ordersMap(vararg orders: Order): Map<Order, RefundsFetchResult> =
        orders.associateWith { RefundsFetchResult.Success(emptyList()) }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): WooPosOrdersViewModel {
        return WooPosOrdersViewModel(
            savedStateHandle = savedStateHandle,
            ordersDataSource = dataSource,
            resourceProvider = resourceProvider,
            childrenToParentEventSender = childrenToParentEventSender,
            retrieveOrderRefunds = retrieveOrderRefunds,
            ordersAnalyticsTracker = ordersAnalyticsTracker,
            orderItemMapper = orderItemMapper,
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
        whenever(resourceProvider.getString(R.string.woopos_search_orders)).thenReturn("Search orders")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_error_title)).thenReturn("Search error")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_error_description))
            .thenReturn("Search error description")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_empty_title)).thenReturn("No results")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_empty_description))
            .thenReturn("No results description")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_auto_draft)).thenReturn("Draft")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_pending)).thenReturn("Pending")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_processing)).thenReturn("Processing")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_on_hold)).thenReturn("On hold")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_failed)).thenReturn("Failed")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_cancelled)).thenReturn("Canceled")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_completed)).thenReturn("Completed")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_refunded)).thenReturn("Refunded")
        whenever(resourceProvider.getString(R.string.woopos_orders_loading_error_message))
            .thenReturn("Please check your connection try again.")
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
        orderItemMapper = WooPosOrderItemMapper(resourceProvider, formatPrice, orderStatusMapper)
    }

    private suspend fun setupDataSourceMocks() {
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessCache(ordersMap(order(1), order(2)))) }
        )
        whenever(dataSource.getOrderById(any())).thenAnswer { invocation ->
            val orderId = invocation.arguments[0] as Long
            Result.success(order(orderId))
        }
        whenever(dataSource.refreshOrderById(any())).thenReturn(Result.success(order()))
    }

    @Test
    fun `given cache and network data, when init, then final state shows network content`() = runTest {
        // GIVEN
        val cached = listOf(order(1))
        val network = listOf(order(2), order(3))
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(ordersMap(*cached.toTypedArray())))
                emit(LoadOrdersResult.SuccessRemote(ordersMap(*network.toTypedArray())))
            }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(2L, 3L)
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(dataSource).loadOrders()
        assertThat(content.selectedDetails?.id).isEqualTo(2L)
    }

    @Test
    fun `given empty cache and non-empty network, when init, then final state shows network content`() = runTest {
        // GIVEN
        val network = listOf(order(10))
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyMap()))
                emit(LoadOrdersResult.SuccessRemote(ordersMap(*network.toTypedArray())))
            }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        val content = state as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(10L)
        assertThat(content.selectedDetails?.id).isEqualTo(10L)
    }

    @Test
    fun `given empty cache and empty network, when init, then final state is Empty`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyMap()))
                emit(LoadOrdersResult.SuccessRemote(emptyMap()))
            }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Empty::class.java)
    }

    @Test
    fun `given data source error, when init, then state is Error`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.Error("boom")) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Error::class.java)
        val error = state as WooPosOrdersState.Error
        assertThat(error.message).isEqualTo("boom")
    }

    @Test
    fun `given initial content, when refresh, then clear cache and update with network result`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        val before = viewModel.state.value as WooPosOrdersState.Content
        val beforeItems = before.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(beforeItems.items.keys.map { it.id }).containsExactly(1L)

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyMap()))
                emit(LoadOrdersResult.SuccessRemote(ordersMap(order(5), order(6))))
            }
        )

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        val after = viewModel.state.value as WooPosOrdersState.Content
        val afterItems = after.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(afterItems.items.keys.map { it.id }).containsExactly(5L, 6L)
        assertThat(after.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        verify(dataSource).clearCache()
        verify(dataSource).loadOrders(false) // Initial load
        verify(dataSource).loadOrders(true) // Refresh
    }

    @Test
    fun `given orders loaded, when selecting an order, then selected id, flags and details update`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2), order(3)))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onOrderSelected(3L)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = state.items as WooPosOrdersState.Content.Items.Loaded
        val selectedFlags = loadedItems.items.keys.associate { it.id to it.isSelected }
        assertThat(selectedFlags[1L]).isFalse()
        assertThat(selectedFlags[2L]).isFalse()
        assertThat(selectedFlags[3L]).isTrue()
        assertThat(state.selectedDetails?.id).isEqualTo(3L)
    }

    @Test
    fun `given selection removed after reload, when refreshing, then first item is auto selected with details`() =
        runTest {
            // GIVEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(100), order(200)))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onOrderSelected(200L)
            advanceUntilIdle()

            whenever(dataSource.loadOrders(any())).thenReturn(
                flow {
                    emit(LoadOrdersResult.SuccessCache(emptyMap()))
                    emit(LoadOrdersResult.SuccessRemote(ordersMap(order(300), order(400))))
                }
            )

            // WHEN
            viewModel.onRefresh()
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value as WooPosOrdersState.Content
            val loadedItems = state.items as WooPosOrdersState.Content.Items.Loaded
            assertThat(loadedItems.items.keys.map { it.id }).containsExactly(300L, 400L)
            assertThat(state.selectedDetails?.id).isEqualTo(300L)
        }

    @Test
    fun `given ViewModel initialized, when onSearchEvent SearchIconClicked, then search input state opens`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.searchInputState).isInstanceOf(WooPosSearchInputState.Open::class.java)
        val openState = state.searchInputState as WooPosSearchInputState.Open
        val hint = openState.input as WooPosSearchInputState.Open.Input.Hint
        assertThat(hint.hint).isEqualTo("Search orders")
        assertThat(openState.requestFocus).isTrue()
        verify(ordersAnalyticsTracker).trackOrdersListSearchButtonTapped()
    }

    @Test
    fun `given search data available, when onSearchEvent Search with query, then searchOrders is called and state updates`() =
        runTest {
            // GIVEN
            val query = "test query"
            val searchResult = listOf(order(10), order(20))
            whenever(dataSource.searchOrders(eq(query), any())).thenReturn(
                SearchOrdersResult.Success(ordersMap(*searchResult.toTypedArray()))
            )
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1)))) }
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
            val content = state as WooPosOrdersState.Content
            val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
            assertThat(loadedItems.items.keys.map { it.id }).containsExactly(10L, 20L)
            assertThat(content.searchInputState).isInstanceOf(WooPosSearchInputState.Open::class.java)
            verify(dataSource).searchOrders(query)
        }

    @Test
    fun `given ViewModel initialized, when onSearchEvent Search with empty query, then loadOrders is called`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onSearchEvent(WooPosSearchUIEvent.Search("", 0))
            advanceUntilIdle()

            // THEN
            verify(dataSource, times(2)).loadOrders()
        }

    @Test
    fun `given search will fail, when search is performed, then Error state is shown with search input state preserved`() =
        runTest {
            // GIVEN
            val query = "test query"
            whenever(dataSource.searchOrders(eq(query), any())).thenReturn(SearchOrdersResult.Error("search failed"))
            whenever(resourceProvider.getString(R.string.woopos_search_orders_error_title))
                .thenReturn("Unable to load orders")
            whenever(resourceProvider.getString(R.string.woopos_search_orders_error_description))
                .thenReturn("Please try again.")
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1)))) }
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
            advanceUntilIdle()

            // WHEN
            viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
            val content = state as WooPosOrdersState.Content
            assertThat(content.items).isInstanceOf(WooPosOrdersState.Content.Items.Error::class.java)
            val error = content.items as WooPosOrdersState.Content.Items.Error
            assertThat(error.title).isEqualTo("Unable to load orders")
            assertThat(error.message).isEqualTo("Please try again.")
            assertThat(content.searchInputState).isInstanceOf(WooPosSearchInputState.Open::class.java)
        }

    @Test
    fun `given more pages, when end reached and loadMore succeeds, then items append and pagination None`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(
            Result.success(ordersMap(order(3), order(4)))
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(1L, 2L, 3L, 4L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(ordersAnalyticsTracker).trackOrdersListNextPageLoaded()
    }

    @Test
    fun `given more pages, when end reached and loadMore fails, then keep items and show pagination error`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.failure(RuntimeException("boom")))

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(1L, 2L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)
    }

    @Test
    fun `given initial load active, when end reached, then do nothing`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(ordersMap(order(1))))
                delay(Long.MAX_VALUE)
            }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfOrdersListReached()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(dataSource, times(0)).loadMore()
    }

    @Test
    fun `given selected order, when appending next page, then selection and details are preserved`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(10), order(20)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(
            Result.success(ordersMap(order(30), order(40)))
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onOrderSelected(20L)
        advanceUntilIdle()
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(10L, 20L, 30L, 40L)
        val selectedFlags = loadedItems.items.keys.associate { it.id to it.isSelected }
        assertThat(selectedFlags[20L]).isTrue()
        assertThat(content.selectedDetails?.id).isEqualTo(20L)
    }

    @Test
    fun `given pagination error, when try again succeeds, then append next page and clear error`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.failure(RuntimeException("boom")))

        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        var content = viewModel.state.value as WooPosOrdersState.Content
        var loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(1L, 2L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)

        // GIVEN
        whenever(dataSource.loadMore()).thenReturn(Result.success(ordersMap(order(3), order(4))))

        // WHEN
        viewModel.onPaginationErrorTryAgain()
        advanceUntilIdle()

        // THEN
        content = viewModel.state.value as WooPosOrdersState.Content
        loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(1L, 2L, 3L, 4L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(dataSource).loadOrders()
        verify(dataSource, times(2)).loadMore()
    }

    @Test
    fun `given on-hold status, when mapped, then status text is titlecased and hyphen replaced`() = runTest {
        // GIVEN
        val base = OrderTestUtils.generateTestOrder(orderId = 42L)
        val withOnHold = base.copy(status = Order.Status.OnHold)
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(withOnHold))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        val item = loadedItems.items.keys.single { it.id == 42L }
        assertThat(item.status.text).isEqualTo("On hold")
    }

    @Test
    fun `given custom hyphenated status, when mapped, then status text is normalized`() = runTest {
        // GIVEN
        val base = OrderTestUtils.generateTestOrder(orderId = 77L)
        val custom = base.copy(status = Order.Status.Custom("awaiting-payment"))
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(custom))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        val item = loadedItems.items.keys.single { it.id == 77L }
        assertThat(item.status.text).isEqualTo("Awaiting payment")
    }

    @Test
    fun `given connector from resources, when mapping date, then connector appears in formatted date`() = runTest {
        // GIVEN
        val one = OrderTestUtils.generateTestOrder(orderId = 11L)
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(one))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        val item = loadedItems.items.keys.single { it.id == 11L }
        assertThat(item.date).contains(" at ")
    }

    @Test
    fun `given orders loaded, when selecting an order, then selectedOrderDetails is populated`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2)))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onOrderSelected(2L)
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        val selectedItemId = loadedItems.items.keys.single { it.isSelected }.id
        assertThat(selectedItemId).isEqualTo(2L)
        assertThat(content.selectedDetails?.id).isEqualTo(2L)
        assertThat(content.selectedDetails?.lineItems).isInstanceOf(
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState.Loaded::class.java
        )
        assertThat(content.selectedDetails?.total).isEqualTo("$106.00")
    }

    @Test
    fun `given selected order, when appending next page, then selectedOrderDetails content remains correct`() =
        runTest {
            // GIVEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(10), order(20)))) }
            )
            whenever(dataSource.hasMorePages).thenReturn(true)
            whenever(dataSource.loadMore()).thenReturn(Result.success(ordersMap(order(30), order(40))))

            // WHEN
            viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onOrderSelected(20L)
            advanceUntilIdle()
            viewModel.onEndOfOrdersListReached()
            advanceUntilIdle()

            // THEN
            val content = viewModel.state.value as WooPosOrdersState.Content
            val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
            val details = content.selectedDetails!!
            assertThat(loadedItems.items.keys.map { it.id }).containsExactly(10L, 20L, 30L, 40L)
            assertThat(details.id).isEqualTo(20L)
            assertThat(details.totalPaid).isEqualTo("$106.00")
        }

    @Test
    fun `given orders reloaded, when refreshing, then first order details are auto-populated`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(100), order(200)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(300), order(400)))) }
        )

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(300L, 400L)
        assertThat(content.selectedDetails?.id).isEqualTo(300L)
        verify(ordersAnalyticsTracker).trackOrdersListPullToRefreshTriggered()
    }

    @Test
    fun `given content loaded, when onEmailReceiptButtonClicked called, then ToEmailReceipt is sent`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(123)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEmailReceiptButtonClicked(123L)
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(ToEmailReceipt(123L))
        verify(ordersAnalyticsTracker).trackOrderDetailsEmailReceiptTapped()
    }

    @Test
    fun `given order with no refunds, when mapped, then breakdown has empty refunds and null net payment`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1)))) }
        )
        whenever(retrieveOrderRefunds.invoke(any(), any())).thenReturn(Result.success(emptyList()))

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val refundsState = content.selectedDetails?.breakdown?.refundsState
        assertThat(refundsState).isEqualTo(RefundsState.Loaded(refunds = emptyList()))
        assertThat(content.selectedDetails?.breakdown?.netPayment).isNull()
    }

    @Test
    fun `given order with refunds, when mapped, then breakdown includes refund amounts and net payment`() = runTest {
        // GIVEN
        val testOrder = order(1)

        val refund1 = Refund(
            id = 1,
            dateCreated = Date(),
            amount = BigDecimal("10.00"),
            reason = null,
            automaticGatewayRefund = false,
            items = emptyList(),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )
        val refund2 = Refund(
            id = 2,
            dateCreated = Date(),
            amount = BigDecimal("5.00"),
            reason = null,
            automaticGatewayRefund = false,
            items = emptyList(),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(
                    LoadOrdersResult.SuccessRemote(
                        mapOf(
                            testOrder to RefundsFetchResult.Success(listOf(refund1, refund2))
                        )
                    )
                )
            }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val refundsState = content.selectedDetails?.breakdown?.refundsState as? RefundsState.Loaded
        val refundRows = refundsState?.refunds
        assertThat(refundRows).hasSize(2)
        assertThat(refundRows?.map { it.amount }).containsExactly("-$10.00", "-$5.00")
        assertThat(refundRows?.map { it.label }).containsExactly("Refund #1", "Refund #2")
        assertThat(content.selectedDetails?.breakdown?.netPayment).isNotNull()
    }

    @Test
    fun `given selected order, when onBackFromSuccessfullySendingEmailReceipt succeeds, then refreshes details`() =
        runTest {
            // GIVEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(100), order(200)))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onOrderSelected(200L)
            advanceUntilIdle()

            whenever(dataSource.refreshOrderById(200L)).thenReturn(Result.success(order(200)))

            // WHEN
            viewModel.onBackFromSuccessfullySendingEmailReceipt()
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value as WooPosOrdersState.Content
            assertThat(state.selectedDetails?.id).isEqualTo(200L)
            verify(dataSource).refreshOrderById(200L)
        }

    @Test
    fun `given selected order, when onBackFromSuccessfullySendingEmailReceipt fails, then details remain unchanged`() =
        runTest {
            // GIVEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2)))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onOrderSelected(1L)
            advanceUntilIdle()
            val before = viewModel.state.value as WooPosOrdersState.Content
            val beforeDetails = before.selectedDetails

            whenever(dataSource.refreshOrderById(1L)).thenReturn(Result.failure(RuntimeException("boom")))

            // WHEN
            viewModel.onBackFromSuccessfullySendingEmailReceipt()
            advanceUntilIdle()

            // THEN
            val after = viewModel.state.value as WooPosOrdersState.Content
            assertThat(after.selectedDetails).isEqualTo(beforeDetails)
            verify(dataSource).refreshOrderById(1L)
        }

    @Test
    fun `given orders loaded, when selecting an order, then tracks OrdersListRowTapped and OrderDetailsLoaded`() =
        runTest {
            // GIVEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2), order(3)))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onOrderSelected(3L)
            advanceUntilIdle()

            // THEN
            verify(ordersAnalyticsTracker).trackOrdersListRowTapped(
                orderId = eq(3L),
                orderStatus = any(),
                listPosition = eq(2),
                createdAtMillis = any()
            )

            verify(ordersAnalyticsTracker).trackOrderDetailsLoaded(
                orderId = eq(3L),
                orderStatus = any(),
                createdAtMillis = any()
            )
        }

    @Test
    fun `when remote load succeeds, then tracks OrdersListFetched`() = runTest {
        // GIVEN
        val cached = listOf(order(1))
        val remote = listOf(order(2), order(3))
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(ordersMap(*cached.toTypedArray())))
                emit(LoadOrdersResult.SuccessRemote(ordersMap(*remote.toTypedArray())))
            }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        verify(ordersAnalyticsTracker).trackOrdersListFetched(any())
    }

    @Test
    fun `when search succeeds, then tracks OrdersListSearchResultsFetched`() = runTest {
        // GIVEN
        val query = "abc"
        val searchResult = listOf(order(10), order(20))
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1)))) }
        )
        whenever(dataSource.searchOrders(eq(query), any())).thenReturn(
            SearchOrdersResult.Success(ordersMap(*searchResult.toTypedArray()))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        // THEN
        verify(ordersAnalyticsTracker).trackOrdersListSearchResultsFetched(any())
    }

    @Test
    fun `given order with zero discount and zero shipping, when mapped, then discount and shipping are absent`() =
        runTest {
            // GIVEN
            val base = order(1)
            val withZeros = base.copy(
                discountTotal = BigDecimal("0.00"),
                shippingTotal = BigDecimal("0.00")
            )

            // WHEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(withZeros))) }
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            // THEN
            val content = viewModel.state.value as WooPosOrdersState.Content
            val breakdown = content.selectedDetails!!.breakdown
            assertThat(breakdown.discount).isNull()
            assertThat(breakdown.shipping).isNull()
        }

    @Test
    fun `given order with non-zero discount and shipping, when mapped, then discount and shipping are formatted`() =
        runTest {
            // GIVEN
            val base = order(2)
            val withValues = base.copy(
                discountTotal = BigDecimal("3.50"),
                shippingTotal = BigDecimal("4.00")
            )

            // WHEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(withValues))) }
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            // THEN
            val content = viewModel.state.value as WooPosOrdersState.Content
            val breakdown = content.selectedDetails!!.breakdown
            assertThat(breakdown.discount).isEqualTo("-$3.50")
            assertThat(breakdown.shipping).isEqualTo("$4.00")
        }

    @Test
    fun `given search returns no results, when search performed, then selectedDetails is null`() = runTest {
        // GIVEN
        val query = "no results query"
        whenever(dataSource.searchOrders(eq(query), any())).thenReturn(
            SearchOrdersResult.Success(emptyMap())
        )

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        assertThat(content.selectedDetails).isNull()
        assertThat(content.items).isInstanceOf(WooPosOrdersState.Content.Items.NothingFound::class.java)
    }

    @Test
    fun `given order selected and search returns results with selected order, when search performed, then selected order preserved`() =
        runTest {
            // GIVEN
            val query = "test query"
            val order1 = order(1)
            val order2 = order(2)
            val searchResults = listOf(order1, order2, order(3))

            whenever(dataSource.searchOrders(eq(query), any())).thenReturn(
                SearchOrdersResult.Success(ordersMap(*searchResults.toTypedArray()))
            )
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order1, order2))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onOrderSelected(2L)
            advanceUntilIdle()

            // WHEN
            viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
            val content = state as WooPosOrdersState.Content
            assertThat(content.selectedDetails?.id).isEqualTo(2L)

            val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
            val selectedItem = loadedItems.items.keys.first { it.isSelected }
            assertThat(selectedItem.id).isEqualTo(2L)
        }

    @Test
    fun `given order selected and search returns results without selected order, when search performed, then first order selected`() =
        runTest {
            // GIVEN
            val query = "test query"
            val order1 = order(1)
            val order2 = order(2)
            val searchResults = listOf(order(3), order(4))

            whenever(dataSource.searchOrders(eq(query), any())).thenReturn(
                SearchOrdersResult.Success(ordersMap(*searchResults.toTypedArray()))
            )
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order1, order2))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onOrderSelected(2L)
            advanceUntilIdle()

            // WHEN
            viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
            val content = state as WooPosOrdersState.Content
            assertThat(content.selectedDetails?.id).isEqualTo(3L)

            val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
            val selectedItem = loadedItems.items.keys.first { it.isSelected }
            assertThat(selectedItem.id).isEqualTo(3L)
        }

    @Test
    fun `given no order selected and search returns results, when search performed, then first order selected`() =
        runTest {
            // GIVEN
            val query = "test query"
            val searchResults = listOf(order(5), order(6))

            whenever(dataSource.searchOrders(eq("empty"), any())).thenReturn(
                SearchOrdersResult.Success(emptyMap())
            )
            whenever(dataSource.searchOrders(eq(query), any())).thenReturn(
                SearchOrdersResult.Success(ordersMap(*searchResults.toTypedArray()))
            )

            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1)))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchEvent(WooPosSearchUIEvent.Search("empty", 5))
            advanceUntilIdle()

            // WHEN
            viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
            val content = state as WooPosOrdersState.Content
            assertThat(content.selectedDetails?.id).isEqualTo(5L)

            val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
            val selectedItem = loadedItems.items.keys.first { it.isSelected }
            assertThat(selectedItem.id).isEqualTo(5L)
        }

    @Test
    fun `given search fails, when search performed, then no order selected`() = runTest {
        // GIVEN
        val query = "test query"
        whenever(dataSource.searchOrders(eq(query), any())).thenReturn(SearchOrdersResult.Error("search failed"))
        whenever(resourceProvider.getString(R.string.woopos_search_orders_error_title))
            .thenReturn("Unable to load orders")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_error_description))
            .thenReturn("Please try again.")
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1)))) }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        val content = state as WooPosOrdersState.Content
        assertThat(content.selectedDetails?.id).isNull()
    }

    @Test
    fun `given content loaded, when onIssueRefundButtonClicked called, then issue refund dialog is shown`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(123)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onIssueRefundButtonClicked(123L)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.dialogState).isInstanceOf(WooPosOrdersState.Content.DialogState.IssueRefund::class.java)
        val dialogState = state.dialogState as WooPosOrdersState.Content.DialogState.IssueRefund
        assertThat(dialogState.orderId).isEqualTo(123L)
    }

    @Test
    fun `given non-Content state, when onIssueRefundButtonClicked called, then state remains unchanged`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.Error("error")) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        val beforeState = viewModel.state.value

        // WHEN
        viewModel.onIssueRefundButtonClicked(123L)
        advanceUntilIdle()

        // THEN
        val afterState = viewModel.state.value
        assertThat(afterState).isEqualTo(beforeState)
        assertThat(afterState).isInstanceOf(WooPosOrdersState.Error::class.java)
    }

    @Test
    fun `given IssueRefund dialog visible, when onIssueRefundDialogDismissed called, then dialog is hidden`() =
        runTest {
            // GIVEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(456)))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIssueRefundButtonClicked(456L)
            advanceUntilIdle()

            val beforeState = viewModel.state.value as WooPosOrdersState.Content
            assertThat(beforeState.dialogState)
                .isInstanceOf(WooPosOrdersState.Content.DialogState.IssueRefund::class.java)

            // WHEN
            viewModel.onIssueRefundDialogDismissed()
            advanceUntilIdle()

            // THEN
            val afterState = viewModel.state.value as WooPosOrdersState.Content
            assertThat(afterState.dialogState).isEqualTo(WooPosOrdersState.Content.DialogState.Hidden)
        }

    @Test
    fun `given non-Content state, when onIssueRefundDialogDismissed called, then state remains unchanged`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(emptyMap())) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        val beforeState = viewModel.state.value

        // WHEN
        viewModel.onIssueRefundDialogDismissed()
        advanceUntilIdle()

        // THEN
        val afterState = viewModel.state.value
        assertThat(afterState).isEqualTo(beforeState)
        assertThat(afterState).isInstanceOf(WooPosOrdersState.Empty::class.java)
    }

    @Test
    fun `given Content state, when showing and hiding dialog, then other state properties remain unchanged`() =
        runTest {
            // GIVEN
            whenever(dataSource.loadOrders(any())).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(100), order(200)))) }
            )
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onOrderSelected(200L)
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosOrdersState.Content
            val initialItems = initialState.items
            val initialSelectedDetails = initialState.selectedDetails
            val initialPullToRefreshState = initialState.pullToRefreshState
            val initialPaginationState = initialState.paginationState
            val initialSearchInputState = initialState.searchInputState

            // WHEN
            viewModel.onIssueRefundButtonClicked(200L)
            advanceUntilIdle()

            // THEN
            var currentState = viewModel.state.value as WooPosOrdersState.Content
            assertThat(currentState.items).isEqualTo(initialItems)
            assertThat(currentState.selectedDetails).isEqualTo(initialSelectedDetails)
            assertThat(currentState.pullToRefreshState).isEqualTo(initialPullToRefreshState)
            assertThat(currentState.paginationState).isEqualTo(initialPaginationState)
            assertThat(currentState.searchInputState).isEqualTo(initialSearchInputState)

            // WHEN
            viewModel.onIssueRefundDialogDismissed()
            advanceUntilIdle()

            // THEN
            currentState = viewModel.state.value as WooPosOrdersState.Content
            assertThat(currentState.items).isEqualTo(initialItems)
            assertThat(currentState.selectedDetails).isEqualTo(initialSelectedDetails)
            assertThat(currentState.pullToRefreshState).isEqualTo(initialPullToRefreshState)
            assertThat(currentState.paginationState).isEqualTo(initialPaginationState)
            assertThat(currentState.searchInputState).isEqualTo(initialSearchInputState)
        }

    @Test
    fun `given completed order, when order details loaded, then Issue Refund action is available`() = runTest {
        // GIVEN
        val testOrder = order(1).copy(status = Order.Status.Completed)

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(testOrder))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        val details = state.selectedDetails!!
        val actions = (details.actionsState as WooPosOrdersState.OrderActionsState.Loaded).actions
        assertThat(actions).anyMatch { it is WooPosOrdersState.OrderAction.IssueRefund }
        assertThat(actions).anyMatch { it is WooPosOrdersState.OrderAction.EmailReceipt }
    }

    @Test
    fun `given pending order, when order details loaded, then Issue Refund and Email Receipt actions are absent`() = runTest {
        // GIVEN
        val testOrder = order(2).copy(status = Order.Status.Pending)

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(testOrder))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        val details = state.selectedDetails!!
        val actions = (details.actionsState as WooPosOrdersState.OrderActionsState.Loaded).actions
        assertThat(actions).noneMatch { it is WooPosOrdersState.OrderAction.IssueRefund }
        assertThat(actions).noneMatch { it is WooPosOrdersState.OrderAction.EmailReceipt }
    }

    @Test
    fun `given processing order, when order details loaded, then Email Receipt is available but Issue Refund is absent`() = runTest {
        // GIVEN
        val testOrder = order(3).copy(status = Order.Status.Processing)

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(testOrder))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        val details = state.selectedDetails!!
        val actions = (details.actionsState as WooPosOrdersState.OrderActionsState.Loaded).actions
        assertThat(actions).noneMatch { it is WooPosOrdersState.OrderAction.IssueRefund }
        assertThat(actions).anyMatch { it is WooPosOrdersState.OrderAction.EmailReceipt }
    }

    @Test
    fun `given refunded order, when order details loaded, then Email Receipt is available but Issue Refund is absent`() = runTest {
        // GIVEN
        val testOrder = order(4).copy(status = Order.Status.Refunded)

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(testOrder))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        val details = state.selectedDetails!!
        val actions = (details.actionsState as WooPosOrdersState.OrderActionsState.Loaded).actions
        assertThat(actions).noneMatch { it is WooPosOrdersState.OrderAction.IssueRefund }
        assertThat(actions).anyMatch { it is WooPosOrdersState.OrderAction.EmailReceipt }
    }

    @Test
    fun `given orderId in SavedStateHandle, when init, then single order mode with detail fetched`() = runTest {
        // GIVEN
        val targetOrderId = 42L
        val savedState = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to targetOrderId))
        whenever(dataSource.getOrderById(targetOrderId)).thenReturn(Result.success(order(targetOrderId)))

        // WHEN
        viewModel = createViewModel(savedStateHandle = savedState)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        assertThat(viewModel.isSingleOrderMode).isTrue()
        assertThat(content.selectedDetails?.id).isEqualTo(targetOrderId)
        verify(dataSource, times(0)).loadOrders(any())
    }

    @Test
    fun `given single order mode, when getOrderById fails, then state is Error`() = runTest {
        // GIVEN
        val targetOrderId = 42L
        val savedState = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to targetOrderId))
        whenever(dataSource.getOrderById(targetOrderId)).thenReturn(Result.failure(RuntimeException("no connection")))

        // WHEN
        viewModel = createViewModel(savedStateHandle = savedState)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Error::class.java)
        val error = state as WooPosOrdersState.Error
        assertThat(error.message).isEqualTo("Please check your connection try again.")
    }

    @Test
    fun `given single order mode error, when retry clicked, then retries loading single order`() = runTest {
        // GIVEN
        val targetOrderId = 42L
        val savedState = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to targetOrderId))
        whenever(dataSource.getOrderById(targetOrderId)).thenReturn(Result.failure(RuntimeException("no connection")))

        viewModel = createViewModel(savedStateHandle = savedState)
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrdersState.Error::class.java)

        // GIVEN retry succeeds
        whenever(dataSource.getOrderById(targetOrderId)).thenReturn(Result.success(order(targetOrderId)))

        // WHEN
        viewModel.onOrdersLoadingErrorRetryButtonClicked()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        assertThat(viewModel.isSingleOrderMode).isTrue()
        assertThat(content.selectedDetails?.id).isEqualTo(targetOrderId)
        verify(dataSource, times(0)).loadOrders(any())
    }

    @Test
    fun `given single order mode, when order loaded, then tracks OrderDetailsLoaded`() = runTest {
        // GIVEN
        val targetOrderId = 42L
        val savedState = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to targetOrderId))
        whenever(dataSource.getOrderById(targetOrderId)).thenReturn(Result.success(order(targetOrderId)))

        // WHEN
        viewModel = createViewModel(savedStateHandle = savedState)
        advanceUntilIdle()

        // THEN
        verify(ordersAnalyticsTracker).trackOrderDetailsLoaded(
            orderId = eq(targetOrderId),
            orderStatus = any(),
            createdAtMillis = any()
        )
    }

    @Test
    fun `when refund dialog is dismissed, then refreshes selected order`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(100), order(200)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onOrderSelected(200L)
        advanceUntilIdle()

        viewModel.onIssueRefundButtonClicked(200L)
        advanceUntilIdle()

        whenever(dataSource.refreshOrderById(200L)).thenReturn(Result.success(order(200)))

        // WHEN
        viewModel.onIssueRefundDialogDismissed()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.dialogState).isEqualTo(WooPosOrdersState.Content.DialogState.Hidden)
        assertThat(state.selectedDetails?.id).isEqualTo(200L)
        verify(dataSource).refreshOrderById(200L)
    }

    @Suppress("LongMethod")
    @Test
    fun `given order with refunds, when ViewRefundDetailsClicked, then RefundDetails dialog is shown with correct data`() = runTest {
        // GIVEN
        val testOrder = order(1).copy(
            items = listOf(
                Order.Item(
                    itemId = 10L,
                    productId = 100L,
                    name = "Test Product",
                    price = BigDecimal("10.00"),
                    sku = "",
                    quantity = 2f,
                    subtotal = BigDecimal("20.00"),
                    subtotalTax = BigDecimal.ZERO,
                    totalTax = BigDecimal.ZERO,
                    total = BigDecimal("20.00"),
                    variationId = 0L,
                    attributesList = emptyList(),
                )
            ),
            paymentMethodTitle = "Cash"
        )
        val refund = Refund(
            id = 1,
            dateCreated = Date(),
            amount = BigDecimal("10.00"),
            reason = "Damaged",
            automaticGatewayRefund = false,
            items = listOf(
                Refund.Item(
                    productId = 100L,
                    quantity = -1,
                    orderItemId = 10L,
                    total = BigDecimal("-10.00"),
                    totalTax = BigDecimal("-1.00"),
                )
            ),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(
                    LoadOrdersResult.SuccessRemote(
                        mapOf(testOrder to RefundsFetchResult.Success(listOf(refund)))
                    )
                )
            }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosOrdersUIEvent.ViewRefundDetailsClicked(refundIndex = 0))
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        val dialogState = state.dialogState
        assertThat(dialogState).isInstanceOf(WooPosOrdersState.Content.DialogState.RefundDetails::class.java)
        val details = dialogState as WooPosOrdersState.Content.DialogState.RefundDetails
        assertThat(details.label).isEqualTo("Refund #1")
        assertThat(details.items).hasSize(1)
        assertThat(details.items.first().name).isEqualTo("Test Product")
        assertThat(details.paymentMethodTitle).isEqualTo("Cash")
    }

    @Test
    fun `given RefundDetails dialog visible, when onRefundDetailsDialogDismissed, then dialog is hidden`() = runTest {
        // GIVEN
        val testOrder = order(1).copy(paymentMethodTitle = "Cash")
        val refund = Refund(
            id = 1,
            dateCreated = Date(),
            amount = BigDecimal("5.00"),
            reason = null,
            automaticGatewayRefund = false,
            items = listOf(
                Refund.Item(
                    productId = 100L,
                    quantity = -1,
                    orderItemId = 10L,
                    total = BigDecimal("-5.00"),
                    totalTax = BigDecimal.ZERO,
                )
            ),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(
                    LoadOrdersResult.SuccessRemote(
                        mapOf(testOrder to RefundsFetchResult.Success(listOf(refund)))
                    )
                )
            }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUIEvent(WooPosOrdersUIEvent.ViewRefundDetailsClicked(refundIndex = 0))
        advanceUntilIdle()

        assertThat((viewModel.state.value as WooPosOrdersState.Content).dialogState)
            .isInstanceOf(WooPosOrdersState.Content.DialogState.RefundDetails::class.java)

        // WHEN
        viewModel.onRefundDetailsDialogDismissed()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.dialogState).isEqualTo(WooPosOrdersState.Content.DialogState.Hidden)
    }

    @Test
    fun `given order with refunds, when ViewRefundDetailsClicked with invalid index, then state is unchanged`() = runTest {
        // GIVEN
        val testOrder = order(1)
        val refund = Refund(
            id = 1,
            dateCreated = Date(),
            amount = BigDecimal("5.00"),
            reason = null,
            automaticGatewayRefund = false,
            items = emptyList(),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(
                    LoadOrdersResult.SuccessRemote(
                        mapOf(testOrder to RefundsFetchResult.Success(listOf(refund)))
                    )
                )
            }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val beforeState = viewModel.state.value as WooPosOrdersState.Content

        // WHEN
        viewModel.onUIEvent(WooPosOrdersUIEvent.ViewRefundDetailsClicked(refundIndex = 99))
        advanceUntilIdle()

        // THEN
        val afterState = viewModel.state.value as WooPosOrdersState.Content
        assertThat(afterState.dialogState).isEqualTo(beforeState.dialogState)
    }

    @Test
    fun `given refund dialog dismissed for order A, when order B selected before refresh completes, then order B remains selected`() = runTest {
        // GIVEN
        val orderA = order(100)
        val orderB = order(200)
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(orderA, orderB))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat((viewModel.state.value as WooPosOrdersState.Content).selectedDetails?.id).isEqualTo(100L)

        viewModel.onIssueRefundButtonClicked(100L)

        whenever(dataSource.refreshOrderById(100L)).thenReturn(Result.success(orderA))
        whenever(retrieveOrderRefunds.invoke(any(), any())).doSuspendableAnswer {
            delay(5000)
            Result.success(emptyList<Refund>())
        }

        // WHEN
        viewModel.onIssueRefundDialogDismissed()

        whenever(retrieveOrderRefunds.invoke(any(), any())).thenReturn(Result.success(emptyList()))
        viewModel.onOrderSelected(200L)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.selectedDetails?.id).isEqualTo(200L)
    }
}
