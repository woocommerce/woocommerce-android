package com.woocommerce.android.wear.ui.orders.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.wear.analytics.AnalyticsTracker
import com.woocommerce.android.wear.extensions.getStateFlow
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@HiltViewModel
class OrdersListViewModel @Inject constructor(
    private val fetchOrders: FetchOrders,
    private val formatOrders: FormatOrderData,
    private val loginRepository: LoginRepository,
    private val analyticsTracker: AnalyticsTracker,
    savedState: SavedStateHandle
) : WearViewModel() {
    @Suppress("ForbiddenComment")
    // TODO: Storing complete ViewState into SavedState can lead to TransactionTooLarge crashes. Only data that can't
    //  be easily recovered, such as user input, should be stored.
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
}
