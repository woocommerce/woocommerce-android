package com.woocommerce.android.ui.woopos.orders

sealed class WooPosRefundUIEvent {
    data object ContinueToReviewClicked : WooPosRefundUIEvent()
    data object BackToSelectItemsClicked : WooPosRefundUIEvent()

    data object ContinueToConfirmRefundClicked : WooPosRefundUIEvent()
    data object DialogDismissed : WooPosRefundUIEvent()
}
