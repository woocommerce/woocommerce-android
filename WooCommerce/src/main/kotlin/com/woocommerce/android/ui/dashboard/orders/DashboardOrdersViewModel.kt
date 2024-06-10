package com.woocommerce.android.ui.dashboard.orders

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.DashboardWidget.Type.ORDERS
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.Factory
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.Content
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.ui.orders.filters.domain.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = Factory::class)
class DashboardOrdersViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val orderListRepository: OrderListRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider,
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val MAX_NUMBER_OF_ORDERS_TO_DISPLAY_IN_CARD = 3
        const val DEFAULT_FILTER_OPTION_STATUS = "all"
    }

    private val defaultFilterOption = OrderStatusOption(
        key = DEFAULT_FILTER_OPTION_STATUS,
        label = resourceProvider.getString(R.string.orderfilters_default_filter_value),
        statusCount = 0,
        isSelected = true
    )

    private val statusOptions = MutableStateFlow(listOf(defaultFilterOption))
    private val _refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(parentViewModel.refreshTrigger, _refreshTrigger)
        .onStart { emit(RefreshEvent()) }
    private val selectedFilter = savedStateHandle.getStateFlow(viewModelScope, DEFAULT_FILTER_OPTION_STATUS)

    val menu = DashboardWidgetMenu(
        items = listOf(
            DashboardWidget.Type.ORDERS.defaultHideMenuEntry {
                parentViewModel.onHideWidgetClicked(ORDERS)
            }
        )
    )

    val button = DashboardWidgetAction(
        titleResource = R.string.dashboard_action_view_all_orders,
        action = ::onNavigateToOrders
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = selectedFilter.flatMapLatest { status ->
        refreshTrigger.map { Pair(status, it) }
    }.transformLatest { (filterStatus, refresh) ->
        val statusFilter = filterStatus
            .takeIf { it != DEFAULT_FILTER_OPTION_STATUS }
            ?.let { Order.Status.fromValue(it) }
        val hasOrders = orderListRepository.hasOrdersLocally(statusFilter)
        if (refresh.isForced || !hasOrders) {
            emit(ViewState.Loading)
        }
        emitAll(
            combine(
                orderListRepository.observeTopOrders(
                    count = MAX_NUMBER_OF_ORDERS_TO_DISPLAY_IN_CARD,
                    isForced = refresh.isForced,
                    statusFilter = statusFilter
                ),
                statusOptions
            ) { result, statusOptions ->
                result.fold(
                    onSuccess = { orders ->
                        Content(
                            orders = orders.map { order ->
                                val status = statusOptions
                                    .firstOrNull { option -> option.key == order.status.value }?.label
                                    ?: order.status.value

                                ViewState.OrderItem(
                                    id = order.id,
                                    number = "#${order.number}",
                                    date = order.dateCreated.formatToMMMdd(),
                                    customerName = order.billingName.ifEmpty {
                                        resourceProvider.getString(R.string.orderdetail_customer_name_default)
                                    },
                                    status = status,
                                    statusColor = order.status.color,
                                    totalPrice = currencyFormatter.formatCurrency(order.total, order.currency)
                                )
                            },
                            filterOptions = statusOptions,
                            selectedFilter = statusOptions.first { it.key == filterStatus }
                        )
                    },
                    onFailure = { error ->
                        ViewState.Error(error.message ?: "")
                    }
                )
            }
        )
    }.asLiveData()

    init {
        viewModelScope.launch {
            statusOptions.tryEmit(
                getOrderStatusFilterOptions()
                    .toMutableList()
                    .apply { add(0, defaultFilterOption) }
            )
        }
    }

    private val Order.Status.color: Int
        get() {
            return when (this) {
                is Order.Status.Processing -> R.color.tag_bg_processing
                is Order.Status.Completed -> R.color.tag_bg_completed
                is Order.Status.Failed -> R.color.tag_bg_failed
                is Order.Status.OnHold -> R.color.tag_bg_on_hold
                else -> R.color.tag_bg_other
            }
        }

    private fun onNavigateToOrders() {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.ORDERS.trackingIdentifier)
        triggerEvent(NavigateToOrders)
    }

    fun onRefresh() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.ORDERS.trackingIdentifier
            )
        )
        _refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    fun onFilterSelected(filter: OrderStatusOption) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.ORDERS.trackingIdentifier)
        selectedFilter.value = filter.key
    }

    fun onOrderClicked(orderId: Long) {
        parentViewModel.trackCardInteracted(DashboardWidget.Type.ORDERS.trackingIdentifier)
        triggerEvent(NavigateToOrderDetails(orderId))
    }

    sealed class ViewState {
        data object Loading : ViewState()
        data class Error(val message: String) : ViewState()
        data class Content(
            val orders: List<OrderItem>,
            val filterOptions: List<OrderStatusOption>,
            val selectedFilter: OrderStatusOption
        ) : ViewState()

        @StringRes
        val title: Int = ORDERS.titleResource

        data class OrderItem(
            val id: Long,
            val number: String,
            val date: String,
            val customerName: String,
            val status: String,
            @ColorRes val statusColor: Int,
            val totalPrice: String
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardOrdersViewModel
    }

    data object NavigateToOrders : MultiLiveEvent.Event()
    data class NavigateToOrderDetails(val orderId: Long) : MultiLiveEvent.Event()
}
