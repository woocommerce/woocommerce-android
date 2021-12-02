package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreationFormViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    fun onOrderStatusSelected(status: Order.Status) = launch(coroutineDispatchers.io) {
        val orderStatus = orderDetailRepository.getOrderStatus(status.value)
        withContext(coroutineDispatchers.main) {
            viewState = viewState.copy(orderStatus = orderStatus)
        }
    }

    @Parcelize
    data class ViewState(
        val orderStatus: Order.OrderStatus? = null
    ) : Parcelable
}
