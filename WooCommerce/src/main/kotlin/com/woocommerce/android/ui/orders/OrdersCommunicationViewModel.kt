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
    private val savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val KEY_CREATED_ORDER_ID_PENDING_SCROLL = "created_order_id_pending_scroll_to_top"
    }

    /**
     * Id of a just-created order. While set, the order list scrolls to the top until this order appears.
     * Kept in the saved state (rather than sent as an event) so it survives the order creation -> order
     * detail -> back to list navigation, configuration changes and process death.
     */
    val createdOrderIdPendingScrollToTop: Long?
        get() = savedStateHandle[KEY_CREATED_ORDER_ID_PENDING_SCROLL]

    fun trashOrder(orderId: Long) {
        triggerEvent(CommunicationEvent.OrderTrashed(orderId))
    }

    fun onOrderCreated(orderId: Long) {
        savedStateHandle[KEY_CREATED_ORDER_ID_PENDING_SCROLL] = orderId
    }

    fun onScrollToTopAfterOrderCreationHandled() {
        savedStateHandle[KEY_CREATED_ORDER_ID_PENDING_SCROLL] = null
    }

    fun notifyOrdersEmpty() {
        triggerEvent(CommunicationEvent.OrdersEmptyNotified)
    }

    fun notifyOrdersLoading() {
        triggerEvent(CommunicationEvent.OrdersLoadingNotified)
    }

    fun notifyOrdersLoaded() {
        triggerEvent(CommunicationEvent.OrdersLoaded)
    }

    fun applyCustomerFilter(customerId: Long) {
        triggerEvent(CommunicationEvent.CustomerFilterRequested(customerId))
    }

    sealed class CommunicationEvent : MultiLiveEvent.Event() {
        data class OrderTrashed(val orderId: Long) : CommunicationEvent()
        data object OrdersEmptyNotified : CommunicationEvent()
        data object OrdersLoadingNotified : CommunicationEvent()
        data object OrdersLoaded : CommunicationEvent()
        data class CustomerFilterRequested(val customerId: Long) : CommunicationEvent()
    }
}
