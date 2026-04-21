package com.woocommerce.android.ui.woopos.orders.list

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.orders.LoadOrdersResult
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.SearchOrdersResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderItemViewState
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderStatusMapper
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrdersListViewModelTest {

    @Rule @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val dataSource: WooPosOrdersDataSource = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val formatPrice: WooPosFormatPrice = mock()
    private val analyticsTracker: WooPosOrdersAnalyticsTracker = mock()
    private lateinit var orderItemMapper: WooPosOrderItemMapper
    private lateinit var viewModel: WooPosOrdersListViewModel

    @Before
    fun setUp() = runTest {
        setupResourceProvider()
        setupFormatPrice()
        orderItemMapper = WooPosOrderItemMapper(
            resourceProvider,
            formatPrice,
            WooPosOrderStatusMapper(resourceProvider, Locale.US)
        )
        setupHappyPathDataSource()
    }

    private fun setupResourceProvider() {
        whenever(resourceProvider.getString(R.string.date_time_connector)).thenReturn("at")
        whenever(resourceProvider.getString(R.string.woopos_search_orders)).thenReturn("Search orders")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_error_title)).thenReturn("Search error")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_error_description))
            .thenReturn("Try again")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_empty_title)).thenReturn("No results")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_empty_description))
            .thenReturn("Try different query")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_completed)).thenReturn("Completed")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_pending)).thenReturn("Pending")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_processing)).thenReturn("Processing")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_on_hold)).thenReturn("On hold")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_failed)).thenReturn("Failed")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_cancelled)).thenReturn("Canceled")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_refunded)).thenReturn("Refunded")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_auto_draft)).thenReturn("Draft")
        whenever(resourceProvider.getString(R.string.woopos_orders_loading_error_message))
            .thenReturn("Check connection")
    }

    private suspend fun setupFormatPrice() {
        whenever(formatPrice(any<BigDecimal>(), any())).thenAnswer { "\$${(it.arguments[0] as BigDecimal).abs()}" }
        whenever(formatPrice(any<BigDecimal>())).thenAnswer { "\$${(it.arguments[0] as BigDecimal).abs()}" }
    }

    private fun setupHappyPathDataSource() {
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(false)
    }

    // region Init / Loading

    @Test
    fun `when init, then first order is auto-selected`() = runTest {
        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.selectedOrderId.value).isEqualTo(1L)
        val items = loadedItems()
        assertThat(items.first().isSelected).isTrue()
    }

    @Test
    fun `given cache and network, when init, then final state reflects network data`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(ordersMap(order(1))))
                emit(LoadOrdersResult.SuccessRemote(ordersMap(order(10), order(20))))
            }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(loadedItems().map { it.id }).containsExactly(10L, 20L)
    }

    @Test
    fun `given empty network result, when init, then state is Empty`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(emptyMap())) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrdersListState.Empty::class.java)
    }

    @Test
    fun `given data source error, when init, then state is Error`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.Error("Network error")) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val error = viewModel.state.value as WooPosOrdersListState.Error
        assertThat(error.message).isEqualTo("Network error")
    }

    @Test
    fun `given single order mode, when init, then loadOrders is not called`() = runTest {
        // WHEN
        viewModel = createViewModel(SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to 42L)))
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrdersListState.Loading::class.java)
        verify(dataSource, times(0)).loadOrders(any())
    }

    // endregion

    // region Selection

    @Test
    fun `given orders loaded, when selecting an order, then isSelected flags update`() = runTest {
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
        val selected = loadedItems().associate { it.id to it.isSelected }
        assertThat(selected).containsEntry(3L, true)
        assertThat(selected).containsEntry(1L, false)
        assertThat(viewModel.selectedOrderId.value).isEqualTo(3L)
    }

    @Test
    fun `given order already selected, when selecting same order, then no state change`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        val stateBefore = viewModel.state.value

        // WHEN
        viewModel.onOrderSelected(1L)

        // THEN
        assertThat(viewModel.state.value).isSameAs(stateBefore)
    }

    // endregion

    // region Refresh

    @Test
    fun `when refresh, then cache is cleared and orders reloaded`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(5), order(6)))) }
        )

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        verify(dataSource).clearCache()
        assertThat(loadedItems().map { it.id }).containsExactly(5L, 6L)
    }

    @Test
    fun `given selected order removed after reload, when refresh, then first item is auto-selected`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1), order(2)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onOrderSelected(2L)
        advanceUntilIdle()

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(10), order(20)))) }
        )

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.selectedOrderId.value).isEqualTo(10L)
    }

    @Test
    fun `when refresh called, then pull-to-refresh analytics tracked`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker).trackOrdersListPullToRefreshTriggered()
    }

    // endregion

    // region Search

    @Test
    fun `when search icon clicked, then search input opens with hint`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        // THEN
        val open = viewModel.state.value.searchInputState as WooPosSearchInputState.Open
        assertThat(open.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
        assertThat(open.requestFocus).isTrue()
        verify(analyticsTracker).trackOrdersListSearchButtonTapped()
    }

    @Test
    fun `when search with query, then filtered results shown`() = runTest {
        // GIVEN
        val query = "test"
        whenever(dataSource.searchOrders(eq(query), any())).thenReturn(
            SearchOrdersResult.Success(ordersMap(order(10)))
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        // THEN
        assertThat(loadedItems().map { it.id }).containsExactly(10L)
        verify(dataSource).searchOrders(query)
    }

    @Test
    fun `when search with empty query, then loadOrders is called instead`() = runTest {
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
    fun `given search fails, when search performed, then Error items shown`() = runTest {
        // GIVEN
        whenever(dataSource.searchOrders(eq("fail"), any())).thenReturn(SearchOrdersResult.Error("oops"))
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search("fail", 4))
        advanceUntilIdle()

        // THEN
        val items = (viewModel.state.value as WooPosOrdersListState.Content).items
        assertThat(items).isInstanceOf(WooPosOrdersListState.Content.Items.Error::class.java)
    }

    @Test
    fun `given search returns empty, when search performed, then NothingFound shown`() = runTest {
        // GIVEN
        whenever(dataSource.searchOrders(eq("none"), any())).thenReturn(
            SearchOrdersResult.Success(emptyMap())
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search("none", 4))
        advanceUntilIdle()

        // THEN
        val items = (viewModel.state.value as WooPosOrdersListState.Content).items
        assertThat(items).isInstanceOf(WooPosOrdersListState.Content.Items.NothingFound::class.java)
    }

    @Test
    fun `when search closed, then search state is Closed and orders reloaded`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Close)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value.searchInputState).isEqualTo(WooPosSearchInputState.Closed)
    }

    @Test
    fun `when search cleared, then hint restored with focus`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Clear)
        advanceUntilIdle()

        // THEN
        val open = viewModel.state.value.searchInputState as WooPosSearchInputState.Open
        assertThat(open.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
        assertThat(open.requestFocus).isTrue()
    }

    // endregion

    // region Pagination

    @Test
    fun `given more pages, when end reached, then next page appended`() = runTest {
        // GIVEN
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.success(ordersMap(order(3), order(4))))
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        assertThat(loadedItems().map { it.id }).containsExactly(1L, 2L, 3L, 4L)
        verify(analyticsTracker).trackOrdersListNextPageLoaded()
    }

    @Test
    fun `given loadMore fails, when end reached, then pagination error shown`() = runTest {
        // GIVEN
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.failure(RuntimeException("timeout")))
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersListState.Content
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)
        assertThat(loadedItems().map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun `given no more pages, when end reached, then loadMore not called`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        verify(dataSource, times(0)).loadMore(any())
    }

    // endregion

    // region Error retry

    @Test
    fun `given error state, when retry clicked, then orders reload`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.Error("boom")) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(dataSource.loadOrders(any())).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersMap(order(1)))) }
        )

        // WHEN
        viewModel.onOrdersLoadingErrorRetryButtonClicked()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrdersListState.Content::class.java)
    }

    // endregion

    // region refreshOrderItem

    @Test
    fun `when refreshOrderItem called, then only that item is updated`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()
        val updatedOrder = order(1).copy(total = BigDecimal("999.00"))
        whenever(dataSource.getOrderById(1L)).thenReturn(Result.success(updatedOrder))

        // WHEN
        viewModel.refreshOrderItem(1L)
        advanceUntilIdle()

        // THEN
        val item = loadedItems().first { it.id == 1L }
        assertThat(item.total).isEqualTo("\$999.00")
    }

    // endregion

    // region Analytics

    @Test
    fun `when order selected, then row tapped analytics tracked`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onOrderSelected(2L)
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker).trackOrdersListRowTapped(
            orderId = eq(2L),
            orderStatus = any(),
            listPosition = eq(1),
            createdAtMillis = any()
        )
    }

    // endregion

    // region Helpers

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = WooPosOrdersListViewModel(
        savedStateHandle = savedStateHandle,
        ordersDataSource = dataSource,
        resourceProvider = resourceProvider,
        ordersAnalyticsTracker = analyticsTracker,
        orderItemMapper = orderItemMapper,
    )

    private fun order(id: Long): Order = OrderTestUtils.generateTestOrder(orderId = id).copy(
        datePaid = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z")
    )

    private fun ordersMap(vararg orders: Order): Map<Order, RefundsFetchResult> =
        orders.associateWith { RefundsFetchResult.Success(emptyList()) }

    private fun loadedItems(): List<OrderItemViewState> {
        val content = viewModel.state.value as WooPosOrdersListState.Content
        return (content.items as WooPosOrdersListState.Content.Items.Loaded).items
    }

    // endregion
}
