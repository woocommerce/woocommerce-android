package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.NavArgs.ORDER_ID
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.ui.orders.FormatOrderData
import com.woocommerce.android.ui.orders.FormatOrderData.OrderItem
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    ordersRepository: OrdersRepository,
    formatOrder: FormatOrderData,
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
        loginRepository.selectedSiteFlow
            .filterNotNull()
            .onEach { site ->
                savedState.get<Long>(ORDER_ID.key)
                    ?.let { ordersRepository.getOrderFromId(site, it) }
                    ?.let { formatOrder(site, it) }
                    .let { presentOrderData(it) }
            }.launchIn(this)
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
