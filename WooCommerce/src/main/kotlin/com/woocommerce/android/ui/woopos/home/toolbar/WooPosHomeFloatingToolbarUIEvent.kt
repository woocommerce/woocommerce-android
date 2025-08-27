package com.woocommerce.android.ui.woopos.home.toolbar

sealed class WooPosHomeFloatingToolbarUIEvent {
    data object OnToolbarMenuClicked : WooPosHomeFloatingToolbarUIEvent()
    data object OnOutsideOfToolbarMenuClicked : WooPosHomeFloatingToolbarUIEvent()
    data object OnCardReaderStatusClicked : WooPosHomeFloatingToolbarUIEvent()
    data class MenuItemClicked(
        val menuItem: WooPosHomeFloatingToolbarState.Menu.MenuItem
    ) : WooPosHomeFloatingToolbarUIEvent()
}
