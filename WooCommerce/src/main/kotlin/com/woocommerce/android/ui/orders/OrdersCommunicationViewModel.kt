package com.woocommerce.android.ui.orders

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * The ViewModel used for operations that are involve both the parent and child fragments of the orders list
 * detail screens.
 *
 * This should be activity scoped, so that it can be shared between fragments
 */
@HiltViewModel
class OrdersCommunicationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    fun trashOrder(orderId: Long) {
        triggerEvent(CommunicationEvent.OrderTrashed(orderId))
    }

    fun notifyOrdersEmpty() {
        triggerEvent(CommunicationEvent.OrdersEmptyNotified)
    }

    fun notifyOrdersLoading() {
        triggerEvent(CommunicationEvent.OrdersLoadingNotified)
    }

    sealed class CommunicationEvent : MultiLiveEvent.Event() {
        data class OrderTrashed(val orderId: Long) : CommunicationEvent()
        data object OrdersEmptyNotified : CommunicationEvent()
        data object OrdersLoadingNotified : CommunicationEvent()
    }
}
