package com.woocommerce.android.wear.ui.orders.details

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.wear.analytics.AnalyticsTracker
import com.woocommerce.android.wear.extensions.getStateFlow
import com.woocommerce.android.wear.extensions.toWearOrder
import com.woocommerce.android.wear.ui.NavArgs.ORDER_ID
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.orders.FormatOrderData
import com.woocommerce.android.wear.ui.orders.FormatOrderData.OrderItem
import com.woocommerce.android.wear.ui.orders.OrdersRepository
import com.woocommerce.android.wear.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Error
import com.woocommerce.android.wear.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Finished
import com.woocommerce.android.wear.viewmodel.WearViewModel
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_ORDER_DETAILS_DATA_FAILED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_ORDER_DETAILS_DATA_REQUESTED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_ORDER_DETAILS_DATA_SUCCEEDED
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_ORDER_DETAILS_OPENED
import com.woocommerce.commons.WearOrderedProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    private val fetchOrderProducts: FetchOrderProducts,
    private val ordersRepository: OrdersRepository,
    private val formatOrderData: FormatOrderData,
    private val loginRepository: LoginRepository,
    private val analyticsTracker: AnalyticsTracker,
    savedState: SavedStateHandle
) : WearViewModel() {
    private val orderId = savedState.get<Long>(ORDER_ID.key) ?: 0

    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        analyticsTracker.track(WATCH_ORDER_DETAILS_OPENED)
        _viewState.update { it.copy(isLoadingOrder = true) }
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach {
                presentOrderData(it)
                requestProductsData(it)
            }.launchIn(this)
    }

    override fun reloadData(withLoading: Boolean) {
        if (_viewState.value.isLoadingProducts) return
        _viewState.update {
            it.copy(
                isLoadingOrder = withLoading,
                isLoadingProducts = withLoading
            )
        }
        launch {
            loginRepository.selectedSite
                ?.let {
                    presentOrderData(it)
                    requestProductsData(it)
                }
        }
    }

    private suspend fun requestProductsData(site: SiteModel) {
        analyticsTracker.track(WATCH_ORDER_DETAILS_DATA_REQUESTED)
        _viewState.update { it.copy(isLoadingProducts = true) }
        fetchOrderProducts(site, orderId)
            .map { productsRequest ->
                when (productsRequest) {
                    is Finished -> {
                        analyticsTracker.track(WATCH_ORDER_DETAILS_DATA_SUCCEEDED)
                        Pair(site, productsRequest.products)
                    }

                    is Error -> {
                        analyticsTracker.track(WATCH_ORDER_DETAILS_DATA_FAILED)
                        Pair(site, null)
                    }

                    else -> null
                }
            }.filterNotNull().onEach { (site, products) ->
                presentOrderData(site, products)
                _viewState.update { it.copy(isLoadingProducts = false) }
            }.launchIn(this)
    }

    private suspend fun presentOrderData(
        site: SiteModel,
        products: List<WearOrderedProduct>? = emptyList()
    ) {
        val formattedOrder = ordersRepository.getOrderFromId(
            selectedSite = site,
            orderId = orderId
        )?.toWearOrder()?.let {
            formatOrderData(site, it, products)
        }

        _viewState.update { it.copy(orderItem = formattedOrder) }
        _viewState.update { it.copy(isLoadingOrder = false) }
    }

    @Parcelize
    data class ViewState(
        val isLoadingOrder: Boolean = false,
        val isLoadingProducts: Boolean = false,
        val orderItem: OrderItem? = null,
    ) : Parcelable
}
