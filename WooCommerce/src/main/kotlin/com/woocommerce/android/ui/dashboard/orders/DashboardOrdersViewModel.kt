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
import com.woocommerce.android.extensions.capitalize
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.DashboardWidget.Type.ORDERS
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toOrderStatus
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.Factory
import com.woocommerce.android.ui.dashboard.orders.DashboardOrdersViewModel.ViewState.Content
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.Locale

@HiltViewModel(assistedFactory = Factory::class)
class DashboardOrdersViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val orderListRepository: OrderListRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val MAX_NUMBER_OF_ORDERS_TO_DISPLAY_IN_CARD = 3
    }

    private val orderStatusMap = MutableSharedFlow<Map<String, Order.OrderStatus>>(extraBufferCapacity = 1)
    private val refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)

    val menu = DashboardWidgetMenu(
        items = listOf(
            DashboardWidget.Type.ORDERS.defaultHideMenuEntry {
                parentViewModel.onHideWidgetClicked(ORDERS)
            }
        )
    )

    val button = DashboardWidgetAction(
        titleResource = R.string.dashboard_action_view_all_orders,
        action = parentViewModel::onNavigateToOrders
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = merge(parentViewModel.refreshTrigger, refreshTrigger)
        .onStart { emit(RefreshEvent())}
        .flatMapLatest {
            orderListRepository.observeTopOrders(
                MAX_NUMBER_OF_ORDERS_TO_DISPLAY_IN_CARD
            )
        }
        .combine(orderStatusMap) { result, statusMap ->
            result.fold(
                onSuccess = { orders ->
                    if (orders.isEmpty()) {
                        ViewState.Loading
                    } else {
                        Content(
                            orders.map {
                                val status = statusMap[it.status.value]?.label
                                    ?: it.status.value.capitalize(Locale.getDefault())

                                ViewState.OrderItem(
                                    number = "#${it.number}",
                                    date = it.dateCreated.formatToMMMdd(),
                                    customerName = it.billingName,
                                    status = status,
                                    statusColor = it.status.color,
                                    totalPrice = currencyFormatter.formatCurrency(it.total, it.currency)
                                )
                            }
                        )
                    }
                },
                onFailure = {
                    ViewState.Error(it.message ?: "")
                }
            )
        }
        .asLiveData()

    init {
        viewModelScope.launch {
            orderStatusMap.tryEmit(
                orderListRepository.getCachedOrderStatusOptions().mapValues { (_, value) ->
                    value.toOrderStatus()
                }
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

    fun onRefresh() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.ORDERS.trackingIdentifier
            )
        )
        refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    sealed class ViewState {
        data object Loading : ViewState()
        data class Error(val message: String) : ViewState()
        data class Content(val orders: List<OrderItem>) : ViewState()

        @StringRes val title: Int = ORDERS.titleResource

        data class OrderItem(
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
}
