package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

class SelectedOrderTrackerViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val _selectedOrderId = MutableLiveData<Pair<Int, Long>>()
    val selectedOrderId: LiveData<Pair<Int, Long>> = _selectedOrderId

    fun selectOrder(orderId: Pair<Int, Long>) {
        _selectedOrderId.value = orderId
    }
}
