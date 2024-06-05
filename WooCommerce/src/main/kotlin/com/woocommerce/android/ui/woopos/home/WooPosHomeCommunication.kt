package com.woocommerce.android.ui.woopos.home

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosHomeBottomUpCommunication @Inject constructor() {
    private val _bottomUpEventsFlow = MutableSharedFlow<WooPosBottomUpEvent>()
    val bottomUpEventsFlow = _bottomUpEventsFlow.asSharedFlow()

    suspend fun sendEventUp(event: WooPosBottomUpEvent) {
        _bottomUpEventsFlow.emit(event)
    }
}

@ActivityRetainedScoped
class WooPosHomeUpBottomCommunication @Inject constructor() {
    private val _upBottomEventsFlow = MutableSharedFlow<WooPosUpBottomEvent>()
    val upBottomEventsFlow = _upBottomEventsFlow.asSharedFlow()

    suspend fun sendEventDown(event: WooPosUpBottomEvent) {
        _upBottomEventsFlow.emit(event)
    }
}

sealed class WooPosBottomUpEvent {
    data object CheckoutClicked : WooPosBottomUpEvent()
    data object BackFromCheckoutToCartClicked : WooPosBottomUpEvent()
}

sealed class WooPosUpBottomEvent {
    data object BackFromCheckoutToCartClicked : WooPosUpBottomEvent()
}
