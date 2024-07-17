package com.woocommerce.android.ui.woopos.root

sealed class WooPosRootUIEvent {
    data object ExitPOSClicked : WooPosRootUIEvent()
    data object ConnectToAReaderClicked : WooPosRootUIEvent()
    data object ExitConfirmationDialogDismissed : WooPosRootUIEvent()
    data object OnBackFromHomeClicked : WooPosRootUIEvent()
    data class OnSuccessfulPayment(val orderId: Long) : WooPosRootUIEvent()
    data class MenuItemSelected(val menuItem: WooPosRootScreenState.Menu.MenuItem) : WooPosRootUIEvent()
}
