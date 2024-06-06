package com.woocommerce.android.ui.woopos.home

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosChildrenToParentCommunication @Inject constructor() :
    WooPosChildrenToParentEventReceiver, WooPosChildrenToParentEventSender {
    private val _events = MutableSharedFlow<ChildToParentEvent>()
    override val events = _events.asSharedFlow()

    override suspend fun sendToParent(event: ChildToParentEvent) {
        _events.emit(event)
    }
}

sealed class ChildToParentEvent {
    data object CheckoutClicked : ChildToParentEvent()
    data object BackFromCheckoutToCartClicked : ChildToParentEvent()
}

interface WooPosChildrenToParentEventReceiver {
    val events: SharedFlow<ChildToParentEvent>
}

interface WooPosChildrenToParentEventSender {
    suspend fun sendToParent(event: ChildToParentEvent)
}
