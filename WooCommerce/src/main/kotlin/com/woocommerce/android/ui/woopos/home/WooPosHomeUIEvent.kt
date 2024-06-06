package com.woocommerce.android.ui.woopos.home

sealed class WooPosHomeUIEvent {
    data object SystemBackClicked : WooPosHomeUIEvent()

    data object ExitConfirmationDialogDismissed : WooPosHomeUIEvent()
    data object ExitConfirmationDialogConfirmed : WooPosHomeUIEvent()
}
