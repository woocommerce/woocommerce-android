package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.NavArgs.ORDER_ID
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.android.ui.orders.ParseOrderData
import com.woocommerce.android.ui.orders.ParseOrderData.OrderItem
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    ordersRepository: OrdersRepository,
    parseOrder: ParseOrderData,
    loginRepository: LoginRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        _viewState.update { it.copy(isLoading = true) }
        launch {
            savedState.get<Long>(ORDER_ID.key)
                ?.let { ordersRepository.getOrderFromId(it) }
                ?.let {
                    val site = loginRepository.selectedSite ?: return@let null
                    parseOrder(site, it)
                }.let { presentOrderData(it) }
        }
    }

    private fun presentOrderData(order: OrderItem?) {
        _viewState.update {
            it.copy(isLoading = false, orderItem = order)
        }
    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false,
        val orderItem: OrderItem? = null
    ) : Parcelable
}
