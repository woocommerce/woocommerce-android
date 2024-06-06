package com.woocommerce.android.ui.woopos.home

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosChildToParentCommunication @Inject constructor() {
    private val _childToParentEventsFlow = MutableSharedFlow<ChildToParentEvent>()
    val childToParentEventsFlow = _childToParentEventsFlow.asSharedFlow()

    suspend fun sendToParent(event: ChildToParentEvent) {
        _childToParentEventsFlow.emit(event)
    }
}

@ActivityRetainedScoped
class WooPosParentToChildrenCommunication @Inject constructor() {
    private val _parentToChildrenEventsFlow = MutableSharedFlow<ParentToChildrenEvent>()
    val parentToChildEventsFlow = _parentToChildrenEventsFlow.asSharedFlow()

    suspend fun sendToChildren(event: ParentToChildrenEvent) {
        _parentToChildrenEventsFlow.emit(event)
    }
}

sealed class ChildToParentEvent {
    data object CheckoutClicked : ChildToParentEvent()
    data object BackFromCheckoutToCartClicked : ChildToParentEvent()
}

sealed class ParentToChildrenEvent {
    data object BackFromCheckoutToCartClicked : ParentToChildrenEvent()
}
