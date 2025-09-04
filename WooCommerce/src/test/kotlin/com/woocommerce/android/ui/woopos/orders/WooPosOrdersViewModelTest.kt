package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrdersViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val dataSource: WooPosOrdersDataSource = mock()

    private lateinit var viewModel: WooPosOrdersViewModel

    private fun order(id: Long = 1L): Order = OrderTestUtils.generateTestOrder(orderId = id)

    @Before
    fun setUp() {
        whenever(dataSource.loadOrders(eq(true))).thenReturn(
            flow { emit(LoadOrdersResult.Success(listOf(order(1), order(2)))) }
        )
    }

    @Test
    fun `given cache and network data, when init, then final state shows network content`() = runTest {
        val cached = listOf(order(1))
        val network = listOf(order(2), order(3))

        whenever(dataSource.loadOrders(eq(true))).thenReturn(
            flow {
                emit(LoadOrdersResult.Success(cached))
                emit(LoadOrdersResult.Success(network))
            }
        )

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(2L, 3L)
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(dataSource).loadOrders(eq(true))
    }

    @Test
    fun `given empty cache and non-empty network, when init, then final state shows network content`() = runTest {
        val network = listOf(order(10))

        whenever(dataSource.loadOrders(eq(true))).thenReturn(
            flow {
                emit(LoadOrdersResult.Success(emptyList())) // cache
                emit(LoadOrdersResult.Success(network)) // network
            }
        )

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Content::class.java)
        val content = state as WooPosOrdersState.Content
        assertThat(content.items.map { it.id }).containsExactly(10L)
    }

    @Test
    fun `given empty cache and empty network, when init, then final state is Empty`() = runTest {
        whenever(dataSource.loadOrders(eq(true))).thenReturn(
            flow {
                emit(LoadOrdersResult.Success(emptyList())) // cache
                emit(LoadOrdersResult.Success(emptyList())) // network
            }
        )

        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Empty::class.java)
    }

    @Test
    fun `given data source error, when init, then state is Error`() = runTest {
        whenever(dataSource.loadOrders(eq(true))).thenReturn(
            flow { emit(LoadOrdersResult.Error("boom")) }
        )
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosOrdersState.Error::class.java)
        val error = state as WooPosOrdersState.Error
        assertThat(error.message).isEqualTo("boom")
    }

    @Test
    fun `given initial content, when refresh, then skip cache and update with network only`() = runTest {
        whenever(dataSource.loadOrders(eq(true))).thenReturn(
            flow { emit(LoadOrdersResult.Success(listOf(order(1)))) }
        )
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()
        val before = viewModel.state.value as WooPosOrdersState.Content
        assertThat(before.items.map { it.id }).containsExactly(1L)

        whenever(dataSource.loadOrders(eq(false))).thenReturn(
            flow { emit(LoadOrdersResult.Success(listOf(order(5), order(6)))) }
        )

        viewModel.refresh()
        advanceUntilIdle()

        val after = viewModel.state.value as WooPosOrdersState.Content
        assertThat(after.items.map { it.id }).containsExactly(5L, 6L)
        assertThat(after.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
        verify(dataSource).loadOrders(eq(false))
    }

    @Test
    fun `given orders loaded, when selecting an order, then selected id and flags update`() = runTest {
        whenever(dataSource.loadOrders(eq(true))).thenReturn(
            flow { emit(LoadOrdersResult.Success(listOf(order(1), order(2), order(3)))) }
        )
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        viewModel.onOrderSelected(3L)
        advanceUntilIdle()

        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.selectedOrderId).isEqualTo(3L)
        val selectedFlags = state.items.associate { it.id to it.isSelected }
        assertThat(selectedFlags[1L]).isFalse()
        assertThat(selectedFlags[2L]).isFalse()
        assertThat(selectedFlags[3L]).isTrue()
    }

    @Test
    fun `given selection removed after reload, when refreshing, then first item is auto selected`() = runTest {
        whenever(dataSource.loadOrders(eq(true))).thenReturn(
            flow { emit(LoadOrdersResult.Success(listOf(order(100), order(200)))) }
        )
        viewModel = WooPosOrdersViewModel(dataSource)
        advanceUntilIdle()

        viewModel.onOrderSelected(200L)
        advanceUntilIdle()

        whenever(dataSource.loadOrders(eq(false))).thenReturn(
            flow { emit(LoadOrdersResult.Success(listOf(order(300), order(400)))) }
        )

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value as WooPosOrdersState.Content
        assertThat(state.items.map { it.id }).containsExactly(300L, 400L)
        assertThat(state.selectedOrderId).isEqualTo(300L)
    }
}
