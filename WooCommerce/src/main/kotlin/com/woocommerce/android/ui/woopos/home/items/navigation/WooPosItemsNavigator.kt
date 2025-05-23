package com.woocommerce.android.ui.woopos.home.items.navigation

import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariableProductNavigationData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class WooPosItemsNavigator @Inject constructor() {
    private val _events = MutableSharedFlow<WooPosItemsScreenNavigationEvent>()
    val events = _events.asSharedFlow()

    suspend fun sendNavigationEvent(event: WooPosItemsScreenNavigationEvent) {
        _events.emit(event)
    }

    sealed class WooPosItemsScreenNavigationEvent {
        data class NavigateToVariationsScreen(val product: WooPosVariableProductNavigationData) : WooPosItemsScreenNavigationEvent()
        data object NavigateBackToItemListScreen : WooPosItemsScreenNavigationEvent()
    }
}
