package com.woocommerce.android.ui.woopos.home

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    data class CheckoutClicked(val productIds: List<Long>) : ChildToParentEvent()
    data object BackFromCheckoutToCartClicked : ChildToParentEvent()
    data class ItemClickedInProductSelector(val productId: Long) : ChildToParentEvent()
    data object NewTransactionClicked : ChildToParentEvent()
    data object OrderSuccessfullyPaid : ChildToParentEvent()
    data object ExitPosClicked : ChildToParentEvent()
    data object ProductsDialogInfoIconClicked : ChildToParentEvent()
    sealed class CartStatusChanged : ChildToParentEvent() {
        data object Empty : CartStatusChanged()
        data object NotEmpty : CartStatusChanged()
    }
    sealed class ProductsStatusChanged : ChildToParentEvent() {
        data object Loading : ProductsStatusChanged()
        data object FullScreen : ProductsStatusChanged()
        data object WithCart : ProductsStatusChanged()
    }
}

interface WooPosChildrenToParentEventReceiver {
    val events: Flow<ChildToParentEvent>
}

interface WooPosChildrenToParentEventSender {
    suspend fun sendToParent(event: ChildToParentEvent)
}
