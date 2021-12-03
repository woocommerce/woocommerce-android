package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState) {
    val orderDraftData = LiveDataDelegate(savedState, Order.EMPTY, onChange = ::onOrderDraftChange)
    private var orderDraft by orderDraftData

    private val orderStatus = MutableLiveData<OrderStatus>()
    val orderStatusData: LiveData<OrderStatus> = orderStatus

    init {
        updateOrderStatus(orderDraft.status)
    }

    fun onOrderStatusChanged(status: Order.Status) {
        orderDraft = orderDraft.copy(status = status)
    }

    private fun onOrderDraftChange(old: Order?, new: Order) {
        if (old?.status != new.status) {
            updateOrderStatus(new.status)
        }
    }

    private fun updateOrderStatus(status: Order.Status) {
        launch {
            orderStatus.value = orderDetailRepository.getOrderStatus(status.value)
        }
    }
}
