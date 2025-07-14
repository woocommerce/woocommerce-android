package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

/**
 * The ViewModel used to communicate between different parts of the Products feature
 * As in tablet mode 2 panes are visible, and handleResult approach is not possible
 * This should be activity scoped, so that it can be shared between fragments
 */
class ProductsCommunicationViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun pushEvent(event: CommunicationEvent) {
        triggerEvent(event)
    }

    sealed class CommunicationEvent : MultiLiveEvent.Event() {
        data class ProductTrashed(val productId: Long) : CommunicationEvent()
        data object ProductUpdated : CommunicationEvent()
        data class ProductChanges(val hasChanges: Boolean) : CommunicationEvent()
        data class ProductSelected(val productId: Long) : CommunicationEvent()
    }
}
