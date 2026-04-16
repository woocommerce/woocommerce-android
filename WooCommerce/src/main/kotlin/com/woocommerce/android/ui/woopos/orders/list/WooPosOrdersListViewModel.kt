package com.woocommerce.android.ui.woopos.orders.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.orders.ORDERS_ROUTE_ORDER_ID_KEY
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersAnalyticsTracker
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val ordersDataSource: WooPosOrdersDataSource,
    private val resourceProvider: ResourceProvider,
    private val ordersAnalyticsTracker: WooPosOrdersAnalyticsTracker,
    private val orderItemMapper: WooPosOrderItemMapper,
) : ViewModel() {

    private val singleOrderId: Long? = savedStateHandle.get<Long>(ORDERS_ROUTE_ORDER_ID_KEY)

    private val _state = MutableStateFlow<WooPosOrdersListState>(
        WooPosOrdersListState.Loading(
            searchInputState = WooPosSearchInputState.Closed
        )
    )
    val state: StateFlow<WooPosOrdersListState> = _state.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<Long?>(null)
    val selectedOrderId: StateFlow<Long?> = _selectedOrderId.asStateFlow()

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    init {
        // Order loading will be moved here from WooPosOrdersViewModel
    }
}
