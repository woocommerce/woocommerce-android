package com.woocommerce.android.ui.dashboard.orders

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.Companion.DEFAULT_FILTER_OPTION_STATUS
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.ui.orders.filters.domain.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardOrdersViewModelTest : BaseUnitTest() {
    private val sampleOrders = List(3) {
        OrderTestUtils.generateTestOrder(orderId = it.toLong())
    }

    private val parentViewModel: DashboardViewModel = mock {
        on { refreshTrigger } doReturn emptyFlow()
    }
    private val orderListRepository: OrderListRepository = mock {
        onBlocking { hasOrdersLocally(anyOrNull()) } doReturn false
        on { observeTopOrders(any(), any(), anyOrNull()) } doReturn flowOf(Result.success(sampleOrders))
    }
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions = mock {
        onBlocking { invoke() } doReturn emptyList()
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(amount = any(), any(), any()) } doAnswer { it.arguments[0].toString() }
    }
    private lateinit var viewModel: DashboardOrdersViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = DashboardOrdersViewModel(
            savedStateHandle = SavedStateHandle(),
            parentViewModel = parentViewModel,
            orderListRepository = orderListRepository,
            resourceProvider = resourceProvider,
            currencyFormatter = currencyFormatter,
            getOrderStatusFilterOptions = getOrderStatusFilterOptions,
            analyticsTrackerWrapper = analyticsTracker,
        )
    }

    @Test
    fun `given no local orders, when card is loaded, then show loading`() = testBlocking {
        setup()

        val initialState = viewModel.viewState.captureValues().first()

        assertThat(initialState).isEqualTo(DashboardOrdersViewModel.ViewState.Loading)
    }

    @Test
    fun `given local orders, when card is loaded, then skip loading`() = testBlocking {
        setup {
            whenever(orderListRepository.hasOrdersLocally()) doReturn true
        }

        val states = viewModel.viewState.captureValues()

        assertThat(states).noneMatch { it == DashboardOrdersViewModel.ViewState.Loading }
    }

    @Test
    fun `given successful loading of orders, when card is loaded, then show recent orders`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState).isInstanceOf(DashboardOrdersViewModel.ViewState.Content::class.java)
        (viewState as DashboardOrdersViewModel.ViewState.Content).orders.forEachIndexed { index, order ->
            assertThat(order.id).isEqualTo(sampleOrders[index].id)
            assertThat(order.number).isEqualTo("#${sampleOrders[index].number}")
            assertThat(order.date).isEqualTo(sampleOrders[index].dateCreated.formatToMMMdd())
            assertThat(order.customerName).isEqualTo(sampleOrders[index].billingName)
            assertThat(order.status).isEqualTo(sampleOrders[index].status.value)
            assertThat(order.totalPrice)
                .isEqualTo(currencyFormatter.formatCurrency(sampleOrders[index].total, sampleOrders[index].currency))
        }
    }

    @Test
    fun `given failure when loading orders, when card is loaded, then show error`() = testBlocking {
        setup {
            whenever(orderListRepository.observeTopOrders(any(), any(), anyOrNull()))
                .thenReturn(flowOf(Result.failure(Exception("Error"))))
        }

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState).isInstanceOf(DashboardOrdersViewModel.ViewState.Error::class.java)
    }

    @Test
    fun `given failure when loading orders, when retry is clicked, then reload orders`() = testBlocking {
        setup {
            whenever(orderListRepository.observeTopOrders(any(), any(), anyOrNull()))
                .thenReturn(flowOf(Result.failure(Exception("Error"))))
                .thenReturn(flowOf(Result.success(sampleOrders)))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onRefresh()
        }.last()

        assertThat(viewState).isInstanceOf(DashboardOrdersViewModel.ViewState.Content::class.java)
    }

    @Test
    fun `when filter options are loaded, then show filter options`() = testBlocking {
        val orderStatusOptions = OrderTestUtils.generateOrderStatusOptions()
            .map {
                OrderStatusOption(
                    key = it.statusKey,
                    label = it.label,
                    statusCount = it.statusCount,
                    isSelected = false
                )
            }
        setup {
            whenever(getOrderStatusFilterOptions.invoke()) doReturn orderStatusOptions
        }

        val viewState = viewModel.viewState.captureValues().last()

        val content = viewState as DashboardOrdersViewModel.ViewState.Content
        assertThat(content.filterOptions).isEqualTo(
            listOf(
                OrderStatusOption(
                    key = DEFAULT_FILTER_OPTION_STATUS,
                    label = resourceProvider.getString(R.string.orderfilters_default_filter_value),
                    statusCount = 0,
                    isSelected = true
                )
            ) + orderStatusOptions
        )
    }

    @Test
    fun `when changing filter, then update orders`() = testBlocking {
        val newFilter = OrderStatusOption(
            key = "new_filter",
            label = "New Filter",
            statusCount = 0,
            isSelected = true
        )
        setup {
            whenever(getOrderStatusFilterOptions.invoke()) doReturn listOf(newFilter)
            whenever(orderListRepository.observeTopOrders(any(), any(), eq(null)))
                .thenReturn(flowOf(Result.success(sampleOrders)))
            whenever(orderListRepository.observeTopOrders(any(), any(), argThat { this.value == newFilter.key }))
                .thenReturn(flowOf(Result.success(sampleOrders.take(2))))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onFilterSelected(newFilter)
        }.last()

        val content = viewState as DashboardOrdersViewModel.ViewState.Content
        assertThat(content.orders).hasSize(2)
    }

    @Test
    fun `when view all is clicked, then navigate to orders`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.button.action()
        }.last()

        assertThat(event).isEqualTo(DashboardOrdersViewModel.NavigateToOrders)
    }

    @Test
    fun `when an order is clicked, then navigate to order details`() = testBlocking {
        setup()

        val order = sampleOrders.first()
        val event = viewModel.event.runAndCaptureValues {
            viewModel.onOrderClicked(order.id)
        }.last()

        assertThat(event).isEqualTo(DashboardOrdersViewModel.NavigateToOrderDetails(order.id))
    }
}
