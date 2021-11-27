package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.util.*
import javax.inject.Inject

@HiltViewModel
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    lateinit var orderDraft: OrderDraft

    fun start() {
        orderDraft = OrderDraft(
            status = Order.Status.Pending,
            dateCreated = Date()
        )
    }

    @Parcelize
    data class OrderDraft(
        val status: Order.Status,
        val dateCreated: Date
    ) : Parcelable
}
