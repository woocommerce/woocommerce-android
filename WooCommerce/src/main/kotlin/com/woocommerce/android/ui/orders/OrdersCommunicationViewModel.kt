package com.woocommerce.android.ui.orders

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

/**
 * The ViewModel used to communicate between different parts of the Orders feature.
 * This should be activity scoped, so that it can be shared between fragments.
 */
class OrdersCommunicationViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun pushEvent(event: CommunicationEvent) {
        triggerEvent(event)
    }

    sealed class CommunicationEvent : MultiLiveEvent.Event() {
        data object OrdersEmpty : CommunicationEvent()
        data object OrdersLoading : CommunicationEvent()
    }
}
