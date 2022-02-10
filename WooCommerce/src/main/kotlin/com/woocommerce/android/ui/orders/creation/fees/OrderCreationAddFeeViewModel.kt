package com.woocommerce.android.ui.orders.creation.fees

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderCreationAddFeeViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onDoneSelected(input: String, isPercentageSelected: Boolean) {

    }

    enum class FeeType {
        AMOUNT, PERCENTAGE
    }

    data class FeeCreationData(
        val inputValue: String,
        val feeType: FeeType
    ) : Event()
}
