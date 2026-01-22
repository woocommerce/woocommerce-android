package com.woocommerce.android.ui.woopos.orders

sealed class WooPosRefundUIEvent {
    data object ContinueToReviewClicked : WooPosRefundUIEvent()
    data object BackToSelectItemsClicked : WooPosRefundUIEvent()
    data class OnRefundReasonChanged(val reason: String) : WooPosRefundUIEvent()
    data object ContinueToConfirmRefundClicked : WooPosRefundUIEvent()
    data object BackToReviewClicked : WooPosRefundUIEvent()
    data object OnRefundConfirmed : WooPosRefundUIEvent()
    data object DialogDismissed : WooPosRefundUIEvent()
}
