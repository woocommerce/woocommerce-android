package com.woocommerce.android.ui.orders.list

import android.os.Parcelable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.extensions.getStateFlow
import com.woocommerce.android.ui.NavRoutes.ORDER_DETAILS
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.ui.orders.FormatOrderData
import com.woocommerce.android.ui.orders.FormatOrderData.OrderItem
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest.Finished
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest.Waiting
import com.woocommerce.android.viewmodel.ScopedViewModel
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
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        _viewState.update { it.copy(isLoading = true) }
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach { requestOrdersData(it) }
            .launchIn(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        launch {
            loginRepository.selectedSite?.let { requestOrdersData(it) }
        }
    }

    fun onOrderItemClick(orderId: Long) {
        navController.navigate(ORDER_DETAILS.withArgs(orderId))
    }

    private suspend fun requestOrdersData(selectedSite: SiteModel) {
        fetchOrders(selectedSite)
            .onEach { request ->
                when (request) {
                    is Finished -> _viewState.update { viewState ->
                        viewState.copy(
                            orders = formatOrders(selectedSite, request.orders),
                            isLoading = false
                        )
                    }
                    is Waiting -> _viewState.update { it.copy(isLoading = true) }
                    else -> _viewState.update { it.copy(isLoading = false) }
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
