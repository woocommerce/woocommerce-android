package com.woocommerce.android.ui.woopos.orders

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosOrdersCoordinator @Inject constructor() {
    private val _selectedOrderId = MutableStateFlow<Long?>(null)
    val selectedOrderId: StateFlow<Long?> = _selectedOrderId.asStateFlow()

    private val _orderRefreshed = MutableSharedFlow<Long>()
    val orderRefreshed: SharedFlow<Long> = _orderRefreshed.asSharedFlow()

    fun selectOrder(orderId: Long) {
        _selectedOrderId.value = orderId
    }

    suspend fun notifyOrderRefreshed(orderId: Long) {
        _orderRefreshed.emit(orderId)
    }
}
