package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.data.WooPosGetOrderRefundsByOrderId
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrdersViewModelTest {

    @Rule @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val dataSource: WooPosOrdersDataSource = mock()

    private lateinit var viewModel: WooPosOrdersViewModel

    private fun order(id: Long = 1L): Order = OrderTestUtils.generateTestOrder(orderId = id)
    private val resourceProvider: ResourceProvider = mock()
    private val getProductById: WooPosGetProductById = mock()
    private val formatPrice: WooPosFormatPrice = mock()
    private val getOrderRefunds: WooPosGetOrderRefundsByOrderId = mock()
    private val providedLocale: Locale = Locale.US
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()

    private fun createViewModel(): WooPosOrdersViewModel {
        return WooPosOrdersViewModel(
            ordersDataSource = dataSource,
            resourceProvider = resourceProvider,
            locale = providedLocale,
            getProductById = getProductById,
            childrenToParentEventSender = childrenToParentEventSender,
            formatPrice = formatPrice,
            getOrderRefunds = getOrderRefunds
        )
    }

    @Before
    fun setUp() {
        whenever(resourceProvider.getString(R.string.date_time_connector)).thenReturn("at")

        runBlocking {
            whenever(formatPrice.invoke(any())).thenReturn("$0.00")
        }

        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessCache(listOf(order(1), order(2)))) }
        )

        whenever(resourceProvider.getString(R.string.woopos_orders_status_auto_draft)).thenReturn("Draft")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_pending)).thenReturn("Pending Payment")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_processing)).thenReturn("Processing")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_on_hold)).thenReturn("On hold")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_failed)).thenReturn("Failed")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_cancelled)).thenReturn("Cancelled")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_completed)).thenReturn("Completed")
        whenever(resourceProvider.getString(R.string.woopos_orders_status_refunded)).thenReturn("Refunded")
        whenever(resourceProvider.getString(R.string.woopos_search_orders)).thenReturn("Search orders")

        runBlocking {
            whenever(getProductById.invoke(any())).thenReturn(null)
            whenever(getOrderRefunds.invoke(any())).thenReturn(emptyList())
        }
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
        assertThat(content.selectedDetails.id).isEqualTo(2L)
    }

    @Test
    fun `given empty cache and non-empty network, when init, then final state shows network content`() = runTest {
        // GIVEN
        val network = listOf(order(10))
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(network))
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
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(10L)
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        assertThat(content.selectedDetails.id).isEqualTo(10L)
    }

    @Test
    fun `given empty cache and empty network, when init, then final state is Empty`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(emptyList()))
                emit(LoadOrdersResult.SuccessRemote(emptyList()))
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
        whenever(dataSource.loadOrders()).thenReturn(
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
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        val before = viewModel.state.value as WooPosOrdersState.Content
        val beforeItems = before.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(beforeItems.items.keys.map { it.id }).containsExactly(1L)

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
        val afterItems = after.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(afterItems.items.keys.map { it.id }).containsExactly(5L, 6L)
        assertThat(after.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        verify(dataSource).clearCache()
        verify(dataSource, times(2)).loadOrders()
    }

    @Test
    fun `given orders loaded, when selecting an order, then selected id, flags and details update`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2), order(3)))) }
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
        assertThat(state.selectedDetails.id).isEqualTo(3L)
    }

    @Test
    fun `given selection removed after reload, when refreshing, then first item is auto selected with details`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(100), order(200)))) }
        )
        viewModel = createViewModel()
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
        val loadedItems = state.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(300L, 400L)
        // first item should be auto-selected after reload
        assertThat(state.selectedDetails.id).isEqualTo(300L)
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
        assertThat(openState.input).isInstanceOf(WooPosSearchInputState.Open.Input.Hint::class.java)
        val hint = openState.input as WooPosSearchInputState.Open.Input.Hint
        assertThat(hint.hint).isEqualTo("Search orders")
        assertThat(openState.requestFocus).isTrue()
    }

    @Test
    fun `given search data available, when onSearchEvent Search with query, then searchOrders is called and state updates`() = runTest {
        // GIVEN
        val query = "test query"
        val searchResult = listOf(order(10), order(20))
        whenever(dataSource.searchOrders(query)).thenReturn(SearchOrdersResult.Success(searchResult))

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
    fun `given ViewModel initialized, when onSearchEvent Search with empty query, then loadOrders is called`() = runTest {
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
    fun `given search will fail, when search is performed, then Error state is shown with search input state preserved`() = runTest {
        // GIVEN
        val query = "test query"
        whenever(dataSource.searchOrders(query)).thenReturn(SearchOrdersResult.Error("search failed"))
        whenever(resourceProvider.getString(R.string.woopos_search_orders_error_title))
            .thenReturn("Unable to load orders")
        whenever(resourceProvider.getString(R.string.woopos_search_orders_error_description))
            .thenReturn("Please try again.")

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
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.success(listOf(order(3), order(4))))

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
    }

    @Test
    fun `given more pages, when end reached and loadMore fails, then keep items and show pagination error`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2)))) }
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
        whenever(dataSource.loadOrders()).thenReturn(
            flow {
                emit(LoadOrdersResult.SuccessCache(listOf(order(1))))
                kotlinx.coroutines.delay(Long.MAX_VALUE)
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
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(10), order(20)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.success(listOf(order(30), order(40))))

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
        assertThat(content.selectedDetails.id).isEqualTo(20L)
    }

    @Test
    fun `given pagination error, when try again succeeds, then append next page and clear error`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.failure(RuntimeException("boom")))

        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfOrdersListReached()
        advanceUntilIdle()

        // THEN (pagination shows error)
        var content = viewModel.state.value as WooPosOrdersState.Content
        var loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(1L, 2L)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)

        // GIVEN (next call will succeed)
        whenever(dataSource.loadMore()).thenReturn(Result.success(listOf(order(3), order(4))))

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
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(withOnHold))) }
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
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(custom))) }
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
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(one))) }
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
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2)))) }
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onOrderSelected(2L)
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        // selected item should be 2, and selectedDetails should match
        val selectedItemId = loadedItems.items.keys.single { it.isSelected }.id
        assertThat(selectedItemId).isEqualTo(2L)
        assertThat(content.selectedDetails.id).isEqualTo(2L)
        assertThat(content.selectedDetails.lineItems).isNotEmpty
        assertThat(content.selectedDetails.total).isEqualTo("$0.00")
    }

    @Test
    fun `given selected order, when appending next page, then selectedOrderDetails content remains correct`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(10), order(20)))) }
        )
        whenever(dataSource.hasMorePages).thenReturn(true)
        whenever(dataSource.loadMore()).thenReturn(Result.success(listOf(order(30), order(40))))

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
        val details = content.selectedDetails
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(10L, 20L, 30L, 40L)
        assertThat(details.id).isEqualTo(20L)
        assertThat(details.totalPaid).isEqualTo("$0.00")
    }

    @Test
    fun `given orders reloaded, when refreshing, then first order details are auto-populated`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(100), order(200)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(300), order(400)))) }
        )

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        val loadedItems = content.items as WooPosOrdersState.Content.Items.Loaded
        assertThat(loadedItems.items.keys.map { it.id }).containsExactly(300L, 400L)
        assertThat(content.selectedDetails.id).isEqualTo(300L)
    }

    @Test
    fun `given content loaded, when onEmailReceiptButtonClicked called, then ToEmailReceipt is sent`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(123)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEmailReceiptButtonClicked(123L)
        advanceUntilIdle()

        // THEN
        verify(childrenToParentEventSender).sendToParent(ToEmailReceipt(123L))
    }

    @Test
    fun `given order with no refunds, when mapped, then breakdown has empty refunds and null net payment`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1)))) }
        )
        runBlocking {
            whenever(getOrderRefunds.invoke(1L)).thenReturn(emptyList())
        }

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.selectedDetails.breakdown.refunds).isEmpty()
        assertThat(content.selectedDetails.breakdown.netPayment).isNull()
    }

    @Test
    fun `given order with refunds, when mapped, then breakdown includes refund amounts and net payment`() = runTest {
        // GIVEN
        val testOrder = order(1)
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(testOrder))) }
        )

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

        runBlocking {
            whenever(getOrderRefunds.invoke(1L)).thenReturn(listOf(refund1, refund2))
            whenever(formatPrice.invoke(java.math.BigDecimal("10.00"))).thenReturn("$10.00")
            whenever(formatPrice.invoke(java.math.BigDecimal("5.00"))).thenReturn("$5.00")
        }

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosOrdersState.Content
        assertThat(content.selectedDetails.breakdown.refunds).containsExactly("-$10.00", "-$5.00")
        assertThat(content.selectedDetails.breakdown.netPayment).isNotNull()
    }

    @Test
    fun `given selected order, when onBackFromSuccesfullySendingEmailReceipt succeeds, then refreshes details and resets isRefreshingSelectedDetails`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(100), order(200)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onOrderSelected(200L)
        advanceUntilIdle()

        whenever(dataSource.refreshOrderById(200L)).thenReturn(Result.success(order(200)))

        // WHEN
        viewModel.onBackFromSuccesfullySendingEmailReceipt()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosOrdersState.Content

        assertThat(state.isRefreshingSelectedDetails).isFalse()
        assertThat(state.selectedDetails.id).isEqualTo(200L)
        verify(dataSource).refreshOrderById(200L)
    }

    @Test
    fun `given selected order, when onBackFromSuccesfullySendingEmailReceipt fails, then subtle flag resets and details unchanged`() = runTest {
        // GIVEN
        whenever(dataSource.loadOrders()).thenReturn(
            flow { emit(LoadOrdersResult.SuccessRemote(listOf(order(1), order(2)))) }
        )
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onOrderSelected(1L)
        advanceUntilIdle()
        val before = viewModel.state.value as WooPosOrdersState.Content
        val beforeDetails = before.selectedDetails

        whenever(dataSource.refreshOrderById(1L)).thenReturn(Result.failure(RuntimeException("boom")))

        // WHEN
        viewModel.onBackFromSuccesfullySendingEmailReceipt()
        advanceUntilIdle()

        // THEN
        val after = viewModel.state.value as WooPosOrdersState.Content

        assertThat(after.isRefreshingSelectedDetails).isFalse()
        assertThat(after.selectedDetails).isEqualTo(beforeDetails)
        verify(dataSource).refreshOrderById(1L)
    }
}
