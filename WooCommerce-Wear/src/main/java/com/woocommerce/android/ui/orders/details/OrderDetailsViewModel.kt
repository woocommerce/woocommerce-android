package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.NavArgs.ORDER_ID
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        launch {
            savedState.get<Long>(ORDER_ID.key)
                ?.let { ordersRepository.getOrderFromId(it) }
                ?.let { /* map OrderEntity to UI model */ }
        }

    }

    @Parcelize
    data class ViewState(
        val isLoading: Boolean = false
    ) : Parcelable
}
