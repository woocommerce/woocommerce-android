package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

class SelectedOrderTrackerViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val _selectedOrderId = MutableLiveData<Long>()
    val selectedOrderId: LiveData<Long> = _selectedOrderId

    fun selectOrder(orderId: Long) {
        _selectedOrderId.value = orderId
    }
}
