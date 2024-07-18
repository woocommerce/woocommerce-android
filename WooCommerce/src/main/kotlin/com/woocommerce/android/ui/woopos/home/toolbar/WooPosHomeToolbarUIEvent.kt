package com.woocommerce.android.ui.woopos.home.toolbar

sealed class WooPosHomeToolbarUIEvent {
    data object OnToolbarMenuClicked : WooPosHomeToolbarUIEvent()
    data object OnOutsideOfToolbarMenuClicked : WooPosHomeToolbarUIEvent()
    data object ConnectToAReaderClicked : WooPosHomeToolbarUIEvent()
    data class MenuItemClicked(val menuItem: WooPosHomeToolbarState.Menu.MenuItem) : WooPosHomeToolbarUIEvent()
}
