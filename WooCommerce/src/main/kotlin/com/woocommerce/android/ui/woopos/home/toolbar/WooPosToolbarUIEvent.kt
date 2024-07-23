package com.woocommerce.android.ui.woopos.home.toolbar

sealed class WooPosToolbarUIEvent {
    data object OnToolbarMenuClicked : WooPosToolbarUIEvent()
    data object OnOutsideOfToolbarMenuClicked : WooPosToolbarUIEvent()
    data object ConnectToAReaderClicked : WooPosToolbarUIEvent()
    data class MenuItemClicked(val menuItem: WooPosToolbarState.Menu.MenuItem) : WooPosToolbarUIEvent()
}
