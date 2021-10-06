package com.woocommerce.android.ui.orders.details.editing

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderEditingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orderEditingRepository: OrderEditingRepository
) : ScopedViewModel(savedState) {
    private val navArgs: OrderDetailFragmentArgs by savedState.navArgs()
    private val order: Order

    private val orderId: Long
        get() = navArgs.orderId.toLong()

    val customerOrderNote: String
        get() = order.customerNote

    init {
        order = orderEditingRepository.getOrder(orderId)
    }

    fun updateCustomerOrderNote(updatedCustomerOrderNote: String) {
        launch {
            orderEditingRepository.updateCustomerOrderNote(orderId, updatedCustomerOrderNote)
        }
    }
}
