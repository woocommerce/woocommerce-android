package com.woocommerce.android.wear.ui.orders.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.wear.analytics.AnalyticsTracker
import com.woocommerce.android.wear.extensions.getStateFlow
import com.woocommerce.android.wear.ui.NavRoutes.ORDER_DETAILS
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.orders.FormatOrderData
import com.woocommerce.android.wear.ui.orders.FormatOrderData.OrderItem
import com.woocommerce.android.wear.ui.orders.list.FetchOrders.OrdersRequest.Finished
import com.woocommerce.android.wear.ui.orders.list.FetchOrders.OrdersRequest.Waiting
import com.woocommerce.android.wear.viewmodel.WearViewModel
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_ORDERS_LIST_DATA_FAILED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_ORDERS_LIST_DATA_REQUESTED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_ORDERS_LIST_DATA_SUCCEEDED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_ORDERS_LIST_OPENED
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel

@HiltViewModel(assistedFactory = OrdersListViewModel.Factory::class)
class OrdersListViewModel @AssistedInject constructor(
    @Assisted private val navController: NavHostController,
    private val fetchOrders: FetchOrders,
    private val formatOrders: FormatOrderData,
    private val loginRepository: LoginRepository,
    private val analyticsTracker: AnalyticsTracker,
    savedState: SavedStateHandle
) : WearViewModel() {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        analyticsTracker.track(WATCH_ORDERS_LIST_OPENED)
        _viewState.update { it.copy(isLoading = true) }
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach { site ->
                _viewState.update { it.copy(isLoading = true) }
                requestOrdersData(site)
            }.launchIn(this)
    }

    override fun reloadData(withLoading: Boolean) {
        if (_viewState.value.isLoading) return
        _viewState.update { it.copy(isLoading = withLoading) }
        launch {
            loginRepository.selectedSite?.let { requestOrdersData(it) }
        }
    }

    fun onOrderItemClick(orderId: Long) {
        navController.navigate(ORDER_DETAILS.withArgs(orderId))
    }

    private suspend fun requestOrdersData(selectedSite: SiteModel) {
        analyticsTracker.track(WATCH_ORDERS_LIST_DATA_REQUESTED)
        fetchOrders(selectedSite)
            .onEach { request ->
                when (request) {
                    is Finished -> _viewState.update { viewState ->
                        analyticsTracker.track(WATCH_ORDERS_LIST_DATA_SUCCEEDED)
                        viewState.copy(
                            orders = formatOrders(selectedSite, request.orders),
                            isError = false,
                            isLoading = false
                        )
                    }
                    is Waiting -> _viewState.update {
                        it.copy(isLoading = true, isError = false)
                    }
                    else -> _viewState.update {
                        analyticsTracker.track(WATCH_ORDERS_LIST_DATA_FAILED)
                        it.copy(isLoading = false, isError = true)
                    }
                }
            }.launchIn(this)
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val orders: List<OrderItem> = emptyList()
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): OrdersListViewModel
    }
}
