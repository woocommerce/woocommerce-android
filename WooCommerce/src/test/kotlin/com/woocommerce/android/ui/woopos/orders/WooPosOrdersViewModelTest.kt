package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrdersViewModelTest {

    @Rule @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val dataSource: WooPosOrdersDataSource = mock()

    private lateinit var viewModel: WooPosOrdersViewModel

    private fun order(id: Long = 1L): Order = OrderTestUtils.generateTestOrder(orderId = id)

    @Before
    fun setUp() {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessCache(listOf(order(1), order(2)))) }
        )
    }

    @Test
    fun `given cache and network data, when init, then final state shows network content`() = runTest {
        // GIVEN
        val cached = listOf(order(1))
        val network = listOf(order(2), order(3))
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(cached))
                emit(LoadOrdersResult.SuccessRemote(network))
            }
        )

        // WHEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(2L, 3L)
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(dataSource).loadOrders()
    }

    @Test
    fun `given empty cache and non-empty network, when init, then final state shows network content`() = runTest {
        // GIVEN
        val network = listOf(order(10))
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList())) // cache → VM sets Loading
                emit(LoadOrdersResult.SuccessRemote(network)) // remote → Content
            }
        )

        // WHEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(10L)
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
    }

    @Test
    fun `given empty cache and empty network, when init, then final state is Empty`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList())) // cache → Loading
                emit(LoadOrdersResult.SuccessRemote(emptyList())) // remote → Empty
            }
        )

        // WHEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Empty::class.java)
    }

    @Test
    fun `given data source error, when init, then state is Error`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.Error("boom")) }
        )

        // WHEN
        viewModel = WooPosOrdersViewModel(dataSource)
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
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1)))) }
        )
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()
        val before = viewModel.state.value as WooPosOrdersState.Content
        assertThat(before.items.map { it.id }).containsExactly(1L)

        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(listOf(order(5), order(6))))
            }
        )

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        val after = viewModel.state.value as WooPosOrdersState.Content
        assertThat(after.items.map { it.id }).containsExactly(5L, 6L)
        assertThat(after.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        verify(dataSource).clearCache()
        verify(dataSource, times(2)).loadOrders() // init + refresh
    }

    @Test
    fun `given orders loaded, when selecting an order, then selected id and flags update`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2), order(3)))) }
        )

        // WHEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()
        viewModel.onOrderSelected(3L)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.selectedOrderId).isEqualTo(3L)
        val selectedFlags = state.items.associate { it.id to it.isSelected }
        assertThat(selectedFlags[1L]).isFalse()
        assertThat(selectedFlags[2L]).isFalse()
        assertThat(selectedFlags[3L]).isTrue()
    }

    @Test
    fun `given selection removed after reload, when refreshing, then first item is auto selected`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(100), order(200)))) }
        )
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()
        viewModel.onOrderSelected(200L)
        advanceUntilIdle()

        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(listOf(order(300), order(400))))
            }
        )

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.items.map { it.id }).containsExactly(300L, 400L)
        assertThat(state.selectedOrderId).isEqualTo(300L)
    }

    @Test
    fun `given ViewModel initialized, when onSearchEvent SearchIconClicked, then search input state opens`() = runTest {
        // GIVEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state.searchInputState).isInstanceOf(WooPosSearchInputState.Open::class.java)
        val openState = state.searchInputState as WooPosSearchInputState.Open
        assertThat(openState.input.text).isEmpty()
        assertThat(openState.requestFocus).isTrue()
    }

    @Test
    fun `given search data available, when onSearchEvent Search with query, then searchOrders is called and state updates`() = runTest {
        // GIVEN
        val query = "test query"
        val searchResult = listOf(order(10), order(20))
        whenever(dataSource.searchOrders(query)).thenReturn(SearchOrdersResult.Success(searchResult))

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(10L, 20L)
        assertThat(content.searchInputState).isInstanceOf(WooPosSearchInputState.Open::class.java)

        verify(dataSource).searchOrders(query)
    }

    @Test
    fun `given ViewModel initialized, when onSearchEvent Search with empty query, then loadOrders is called`() = runTest {
        // GIVEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search("", 0))
        advanceUntilIdle()

        // THEN
        verify(dataSource, times(2)).loadOrders() // init + search with empty query
    }

    @Test
    fun `given search input is open, when onSearchEvent Close, then search input state closes and loadOrders is called`() = runTest {
        // GIVEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Close)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state.searchInputState).isEqualTo(WooPosSearchInputState.Closed)
        verify(dataSource, times(2)).loadOrders() // init + close search
    }

    @Test
    fun `given search will fail, when search is performed, then Error state is shown with search input state preserved`() = runTest {
        // GIVEN
        val query = "test query"
        whenever(dataSource.searchOrders(query)).thenReturn(SearchOrdersResult.Error("search failed"))

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        advanceUntilIdle()

        // WHEN
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Error::class.java)
        val error = state as WooPosOrdersState.Error
        assertThat(error.message).isEqualTo("search failed")
        assertThat(error.searchInputState).isInstanceOf(WooPosSearchInputState.Open::class.java)
    }

    @Test
    fun `given more pages, when end reached and loadMore succeeds, then items append and pagination None`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.success(listOf(order(3), order(4))))

        // WHEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(1L, 2L, 3L, 4L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
    }

    @Test
    fun `given more pages, when end reached and loadMore fails, then keep items and show pagination error`() = runTest {
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.failure(RuntimeException("boom")))

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(1L, 2L) // unchanged
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)
    }

    @Test
    fun `given initial load active, when end reached, then do nothing`() = runTest {
        // GIVEN: cache -> Content; remote never completes (keeps loadingJob active)
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(listOf(order(1))))
                kotlinx.coroutines.delay(Long.MAX_VALUE)
            }
        )

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfOrdersListReached()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(dataSource, times(0)).loadMore()
    }

    @Test
    fun `given selected order, when appending next page, then selection is preserved`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(10), order(20)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.success(listOf(order(30), order(40))))

        // WHEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        viewModel.onOrderSelected(20L)
        advanceUntilIdle()

        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(10L, 20L, 30L, 40L)
        assertThat(content.selectedOrderId).isEqualTo(20L)
        val selectedFlags = content.items.associate { it.id to it.isSelected }
        assertThat(selectedFlags[20L]).isTrue()
    }

    @Test
    fun `given search results and more pages, when end reached, then loadMore with query appends and pagination None`() = runTest {
        // GIVEN
        val query = "abc"
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(emptyList())) }
        )
        whenever(dataSource.searchOrders(query)).thenReturn(
            SearchOrdersResult.Success(listOf(order(10), order(20)))
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore(query)).thenReturn(
            Result.success(listOf(order(30), order(40)))
        )

        // WHEN
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()
        viewModel.onSearchEvent(WooPosSearchUIEvent.SearchIconClicked)
        viewModel.onSearchEvent(WooPosSearchUIEvent.Search(query, query.length))
        advanceUntilIdle()

        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(10L, 20L, 30L, 40L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(dataSource).loadMore(query)
    }

    @Test
    fun `given pagination error, when try again succeeds, then append next page and clear error`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)

        whenever(dataSource.loadMore()).thenReturn(
            Result.failure(RuntimeException("boom"))
        )

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        var content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(1L, 2L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)

        whenever(dataSource.loadMore()).thenReturn(Result.success(listOf(order(3), order(4))))
        viewModel.onPaginationErrorTryAgain()
        advanceUntilIdle()

        // THEN
        content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(1L, 2L, 3L, 4L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)

        verify(dataSource).loadOrders()
        verify(dataSource, times(2)).loadMore()
    }

    @Test
    fun `given Error, when retry tapped, then load orders again`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.Error("boom")) }
        )

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(listOf(order(7), order(8))))
            }
        )

        // WHEN
        viewModel.onOrdersLoadingErrorRetryButtonClicked()
        advanceUntilIdle()

        // THEN
        val after = viewModel.state.value as WooPosOrdersState.Content
        assertThat(after.items.map { it.id }).containsExactly(7L, 8L)
        assertThat(after.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        assertThat(after.paginationState).isEqualTo(WooPosPaginationState.None)

        verify(dataSource, times(2)).loadOrders()
    }
}
