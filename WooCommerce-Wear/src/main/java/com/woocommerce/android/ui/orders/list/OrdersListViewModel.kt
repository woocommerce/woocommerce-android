package com.woocommerce.android.ui.orders.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.NavRoutes.ORDER_DETAILS
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.ui.orders.ParseOrderData
import com.woocommerce.android.ui.orders.ParseOrderData.OrderItem
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel

@HiltViewModel(assistedFactory = OrdersListViewModel.Factory::class)
class OrdersListViewModel @AssistedInject constructor(
    @Assisted private val navController: NavHostController,
    private val fetchOrders: FetchOrders,
    private val parseOrders: ParseOrderData,
    loginRepository: LoginRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach { requestOrdersData(it) }
            .launchIn(this)
    }

    fun onOrderItemClick(orderId: Long) {
        navController.navigate(ORDER_DETAILS.withArgs(orderId))
    }

    private suspend fun requestOrdersData(selectedSite: SiteModel) {
        _viewState.update { it.copy(isLoading = true) }
        fetchOrders(selectedSite)
            .onEach { orders ->
                _viewState.update { viewState ->
                    viewState.copy(
                        orders = parseOrders(selectedSite, orders),
                        isLoading = false
                    )
                }
            }.launchIn(this)
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        val orders: List<OrderItem> = emptyList()
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): OrdersListViewModel
    }
}
