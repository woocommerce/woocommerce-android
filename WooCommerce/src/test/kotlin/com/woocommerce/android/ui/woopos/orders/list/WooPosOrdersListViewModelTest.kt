package com.woocommerce.android.ui.woopos.orders.list

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.LoadOrdersResult
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.SearchOrdersResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersCoordinator
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
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
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker = mock()
    private val coordinator = WooPosOrdersCoordinator()
    private lateinit var orderItemMapper: WooPosOrderItemMapper
    private lateinit var orderStatusMapper: WooPosOrderStatusMapper
    private lateinit var viewModel: WooPosOrdersListViewModel
    private val providedLocale: Locale = Locale.US

    private fun order(id: Long = 1L): Order = OrderTestUtils.generateTestOrder(orderId = id).copy(
        datePaid = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z")
    )

    private fun ordersList(vararg orders: Order): List<Order> = orders.toList()

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): WooPosOrdersListViewModel {
        return WooPosOrdersListViewModel(
            savedStateHandle = savedStateHandle,
            ordersDataSource = dataSource,
            resourceProvider = resourceProvider,
            ordersAnalyticsTracker = ordersAnalyticsTracker,
            orderItemMapper = orderItemMapper,
            coordinator = coordinator,
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
    }

    private fun setupMappers() {
        orderStatusMapper = WooPosOrderStatusMapper(resourceProvider, providedLocale)
        orderItemMapper = WooPosOrderItemMapper(resourceProvider, formatPrice, orderStatusMapper)
    }

    private suspend fun setupDataSourceMocks() {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessCache(ordersList(order(1), order(2)))) }
        )
        whenever(dataSource.getOrderById(any())).thenAnswer { invocation ->
            val orderId = invocation.arguments[0] as Long
            Result.success(order(orderId))
        }
    }

    // region Init / Loading

    @Test
    fun `given cache and network data, when init, then final state shows network content`() = runTest {
        val cached = listOf(order(1))
        val network = listOf(order(2), order(3))
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(ordersList(*cached.toTypedArray())))
                emit(LoadOrdersResult.SuccessRemote(ordersList(*network.toTypedArray())))
            }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersListState.Content::class.java)
        val content = state as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        assertThat(loadedItems.items.map { it.id }).containsExactly(2L, 3L)
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        assertThat(coordinator.selectedOrderId.value).isEqualTo(2L)
    }

    @Test
    fun `given empty cache and non-empty network, when init, then final state shows network content`() = runTest {
        val network = listOf(order(10))
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(ordersList(*network.toTypedArray())))
            }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        val content = state as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        assertThat(loadedItems.items.map { it.id }).containsExactly(10L)
        assertThat(coordinator.selectedOrderId.value).isEqualTo(10L)
    }

    @Test
    fun `given empty cache and empty network, when init, then final state is Empty`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(emptyList()))
            }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value).isInstanceOf(WooPosOrdersListState.Empty::class.java)
    }

    @Test
    fun `given data source error, when init, then state is Error`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.Error("boom")) }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersListState.Error::class.java)
        assertThat((state as WooPosOrdersListState.Error).message).isEqualTo("boom")
    }

    @Test
    fun `given single order mode, when init, then loadOrders is not called`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf(ORDERS_ROUTE_ORDER_ID_KEY to 42L))

        viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isInstanceOf(WooPosOrdersListState.Loading::class.java)
        verify(dataSource, times(0)).loadOrders()
    }

    // endregion

    // region Refresh

    @Test
    fun `given empty state, when empty action clicked, then refresh orders`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(emptyList()))
            }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosOrdersListState.Empty::class.java)

        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )

        viewModel.onOrdersEmptyActionClicked()
        advanceUntilIdle()

        verify(dataSource).clearCache()
        val content = viewModel.state.value as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        assertThat(loadedItems.items.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `given initial content, when refresh, then clear cache and update with network result`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(ordersList(order(5), order(6))))
            }
        )

        viewModel.onRefresh()
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        assertThat(loadedItems.items.map { it.id }).containsExactly(5L, 6L)
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        verify(dataSource).clearCache()
    }

    @Test
    fun `given selection removed after reload, when refreshing, then first item is auto selected`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(100), order(200)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onOrderSelected(200L, WooPosScreenType.DualPane)
        advanceUntilIdle()

        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(ordersList(order(300), order(400))))
            }
        )

        viewModel.onRefresh()
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        assertThat(loadedItems.items.map { it.id }).containsExactly(300L, 400L)
        assertThat(coordinator.selectedOrderId.value).isEqualTo(300L)
    }

    // endregion

    // region Selection

    @Test
    fun `given orders loaded, when selecting an order, then isSelected flags update`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1), order(2), order(3)))) }
        )

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onOrderSelected(3L, WooPosScreenType.DualPane)
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        val selectedFlags = loadedItems.items.associate { it.id to it.isSelected }
        assertThat(selectedFlags[1L]).isFalse()
        assertThat(selectedFlags[2L]).isFalse()
        assertThat(selectedFlags[3L]).isTrue()
        assertThat(coordinator.selectedOrderId.value).isEqualTo(3L)
    }

    @Test
    fun `given orders loaded, when selecting order in SinglePane mode, then coordinator selection is not updated`() =
        runTest {
            // GIVEN
            whenever(dataSource.loadOrders()).thenReturn(
                flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1), order(2), order(3)))) }
            )

            viewModel = createViewModel()
            advanceUntilIdle()
            val initialSelectedId = coordinator.selectedOrderId.value

            // WHEN
            viewModel.onOrderSelected(3L, WooPosScreenType.SinglePane)
            advanceUntilIdle()

            // THEN
            assertThat(coordinator.selectedOrderId.value).isEqualTo(initialSelectedId)
            val content = viewModel.state.value as WooPosOrdersListState.Content
            val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
            val selectedFlags = loadedItems.items.filter { it.isSelected }
            assertThat(selectedFlags.map { it.id }).doesNotContain(3L)
            verify(ordersAnalyticsTracker).trackOrdersListRowTapped(
                orderId = eq(3L),
                orderStatus = any(),
                listPosition = eq(2),
                createdAtMillis = any()
            )
        }

    @Test
    fun `given order already selected, when selecting same order, then no state change`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1), order(2)))) }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val stateBefore = viewModel.state.value
        viewModel.onOrderSelected(1L, WooPosScreenType.DualPane)
        advanceUntilIdle()

        assertThat(viewModel.state.value).isSameAs(stateBefore)
    }

    // endregion

    // region Search

    @Test
    fun `given ViewModel initialized, when search icon clicked, then search input state opens`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        assertThat(content.searchInputState).isInstanceOf(WooPosSearchInputState.Open::class.java)
        val openState = content.searchInputState as WooPosSearchInputState.Open
        val hint = openState.input as WooPosSearchInputState.Open.Input.Hint
        assertThat(hint.hint).isEqualTo("Search orders")
        assertThat(openState.requestFocus).isTrue()
        verify(ordersAnalyticsTracker).trackOrdersListSearchButtonTapped()
    }

    @Test
    fun `given search data available, when search with query, then results shown`() = runTest {
        val query = "test query"
        val searchResult = listOf(order(10), order(20))
        whenever(dataSource.searchOrders(eq(query))).thenReturn(
            SearchOrdersResult.Success(ordersList(*searchResult.toTypedArray()))
        )
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        assertThat(loadedItems.items.map { it.id }).containsExactly(10L, 20L)
        assertThat(content.searchInputState).isInstanceOf(WooPosSearchInputState.Open::class.java)
        verify(dataSource).searchOrders(query)
    }

    @Test
    fun `given search with empty query, when search event, then loadOrders is called`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.Search("", 0))
        advanceUntilIdle()

        verify(dataSource, times(2)).loadOrders()
    }

    @Test
    fun `given search will fail, when search is performed, then Error items shown`() = runTest {
        val query = "test query"
        whenever(dataSource.searchOrders(eq(query))).thenReturn(SearchOrdersResult.Error("search failed"))
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        assertThat(content.items).isInstanceOf(WooPosOrdersListState.Content.Items.Error::class.java)
        val error = content.items as WooPosOrdersListState.Content.Items.Error
        assertThat(error.title).isEqualTo("Search error")
        assertThat(error.message).isEqualTo("Search error description")
    }

    @Test
    fun `given search returns empty, when search performed, then NothingFound shown`() = runTest {
        val query = "no results"
        whenever(dataSource.searchOrders(eq(query))).thenReturn(
            SearchOrdersResult.Success(emptyList())
        )
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        assertThat(content.items).isInstanceOf(WooPosOrdersListState.Content.Items.NothingFound::class.java)
    }

    @Test
    fun `given order selected, when search returns empty, then coordinator selection cleared`() = runTest {
        // GIVEN
        val query = "no results"
        whenever(dataSource.searchOrders(eq(query))).thenReturn(
            SearchOrdersResult.Success(emptyList())
        )
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onOrderSelected(1L, WooPosScreenType.DualPane)
        advanceUntilIdle()
        assertThat(coordinator.selectedOrderId.value).isEqualTo(1L)

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        // THEN
        assertThat(coordinator.selectedOrderId.value).isNull()
    }

    @Test
    fun `given search open, when close event, then search state closed and orders reloaded`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.Close)
        advanceUntilIdle()

        assertThat(viewModel.state.value.searchInputState).isEqualTo(WooPosSearchInputState.Closed)
    }

    @Test
    fun `given search open, when clear event, then search hint restored and orders reloaded`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.Clear)
        advanceUntilIdle()

        val searchState = viewModel.state.value.searchInputState as WooPosSearchInputState.Open
        assertThat(searchState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
        assertThat(searchState.requestFocus).isTrue()
    }

    // endregion

    // region Pagination

    @Test
    fun `given more pages, when end reached and loadMore succeeds, then items append`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(
            Result.success(ordersList(order(3), order(4)))
        )

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        assertThat(loadedItems.items.map { it.id }).containsExactly(1L, 2L, 3L, 4L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(ordersAnalyticsTracker).trackOrdersListNextPageLoaded()
    }

    @Test
    fun `given more pages, when end reached and loadMore fails, then show pagination error`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.failure(RuntimeException("boom")))

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        assertThat(loadedItems.items.map { it.id }).containsExactly(1L, 2L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)
    }

    @Test
    fun `given no more pages, when end reached, then no loadMore call`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(false)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        verify(dataSource, times(0)).loadMore(any())
    }

    // endregion

    // region Error retry

    @Test
    fun `given error state, when retry clicked, then reload orders`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.Error("boom")) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )

        viewModel.onOrdersLoadingErrorRetryButtonClicked()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersListState.Content::class.java)
    }

    // endregion

    // region refreshOrderItem

    @Test
    fun `given orders loaded, when order refreshed via coordinator, then specific item updated`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1), order(2)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        val updatedOrder = order(1).copy(total = BigDecimal("999.00"))
        whenever(dataSource.getOrderById(1L)).thenReturn(Result.success(updatedOrder))

        coordinator.notifyOrderRefreshed(1L)
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersListState.Content
        val loadedItems = content.items as WooPosOrdersListState.Content.Items.Loaded
        val item = loadedItems.items.first { it.id == 1L }
        assertThat(item.total).isEqualTo("$999.00")
    }

    // endregion

    // region Analytics

    @Test
    fun `when refresh called, then pull-to-refresh analytics tracked`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRefresh()
        advanceUntilIdle()

        verify(ordersAnalyticsTracker).trackOrdersListPullToRefreshTriggered()
    }

    @Test
    fun `when order selected, then row tapped analytics tracked`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(ordersList(order(1), order(2)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onOrderSelected(2L, WooPosScreenType.DualPane)
        advanceUntilIdle()

        verify(ordersAnalyticsTracker).trackOrdersListRowTapped(
            orderId = eq(2L),
            orderStatus = any(),
            listPosition = eq(1),
            createdAtMillis = any()
        )
    }

    // endregion
}
