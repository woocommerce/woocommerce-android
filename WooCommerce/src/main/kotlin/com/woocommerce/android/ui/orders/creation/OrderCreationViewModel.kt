package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.util.*
import javax.inject.Inject

@HiltViewModel
class OrderCreationViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    fun start() {
        viewState = viewState.copy(
            orderDraft = OrderDraft(
                status = Order.Status.Pending,
                dateCreated = Date()
            )
        )
    }

    @Parcelize
    data class OrderDraft(
        val status: Order.Status,
        val dateCreated: Date
    ) : Parcelable

    @Parcelize
    data class ViewState(
        val orderDraft: OrderDraft? = null
    ) : Parcelable
}
