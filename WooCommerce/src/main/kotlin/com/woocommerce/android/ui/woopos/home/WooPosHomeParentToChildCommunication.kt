package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.products.WooPosProductsListItem
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosParentToChildrenCommunication @Inject constructor() :
    WooPosParentToChildrenEventReceiver, WooPosParentToChildrenEventSender {
    private val _events = MutableSharedFlow<ParentToChildrenEvent>()
    override val events = _events.asSharedFlow()

    override suspend fun sendToChildren(event: ParentToChildrenEvent) {
        _events.emit(event)
    }
}

sealed class ParentToChildrenEvent {
    data object BackFromCheckoutToCartClicked : ParentToChildrenEvent()
    data class ProductSelectionChangedInProductSelector(val selectedItems: List<WooPosProductsListItem>) :
        ParentToChildrenEvent()
}

interface WooPosParentToChildrenEventReceiver {
    val events: SharedFlow<ParentToChildrenEvent>
}

interface WooPosParentToChildrenEventSender {
    suspend fun sendToChildren(event: ParentToChildrenEvent)
}
