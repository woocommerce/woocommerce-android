package com.woocommerce.android.ui.woopos.root

sealed class WooPosRootUIEvent {
    data object OnToolbarMenuClicked : WooPosRootUIEvent()
    data object OnOutsideOfToolbarMenuClicked : WooPosRootUIEvent()
    data object ConnectToAReaderClicked : WooPosRootUIEvent()
    data object ExitConfirmationDialogDismissed : WooPosRootUIEvent()
    data object OnBackFromHomeClicked : WooPosRootUIEvent()
    data class OnSuccessfulPayment(val orderId: Long) : WooPosRootUIEvent()
    data class MenuItemClicked(val menuItem: WooPosRootScreenState.Menu.MenuItem) : WooPosRootUIEvent()
}
