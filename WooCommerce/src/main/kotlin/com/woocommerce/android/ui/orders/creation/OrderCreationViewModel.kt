package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val orderDraftData = LiveDataDelegate(savedState, Order.EMPTY)
    private var orderDraft by orderDraftData

    init {
        orderDraft = orderDraft.copy(
            dateCreated = Date()
        )
    }

    fun onOrderStatusChanged(status: Order.Status) {
        orderDraft = orderDraft.copy(status = status)
    }
}
