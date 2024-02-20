package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

/**
 * `SelectedOrderTrackerViewModel` is designed for managing the state of selected order items within a two-pane layout interface.
 * It tracks the currently selected order item and ensures the UI accurately reflects this selection.
 *
 * Features:
 * - Holds the ID of the currently selected order item, allowing various components to react to changes in selection.
 * - Provides a LiveData to observe and respond to the selection changes, ensuring the selected order item is highlighted appropriately.
 *
 * Usage:
 * - Use `selectOrder(orderId: Long)` to update the currently selected order item.
 * - Observe `selectedOrderId` to respond to changes in the selection and update the UI accordingly.
 */
class SelectedOrderTrackerViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val _selectedOrderId = MutableLiveData<Long>()
    val selectedOrderId: LiveData<Long> = _selectedOrderId

    private val _refreshOrders = MutableLiveData<Unit>()
    val refreshOrders: LiveData<Unit> = _refreshOrders

    fun selectOrder(orderId: Long) {
        _selectedOrderId.value = orderId
    }

    fun refreshOrders() {
        _refreshOrders.value = Unit
    }
}
