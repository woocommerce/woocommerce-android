package com.woocommerce.android.ui.woopos.orders.details.refund

sealed class WooPosRefundUIEvent {
    data object RefundFlowOpened : WooPosRefundUIEvent()
    data class ItemSelectionToggled(val uniqueId: String) : WooPosRefundUIEvent()
    data object SelectAllToggled : WooPosRefundUIEvent()
    data object ContinueToReviewClicked : WooPosRefundUIEvent()
    data object BackToSelectItemsClicked : WooPosRefundUIEvent()
    data class OnRefundReasonChanged(val reason: String) : WooPosRefundUIEvent()
    data object ContinueToConfirmRefundClicked : WooPosRefundUIEvent()
    data object BackToReviewClicked : WooPosRefundUIEvent()
    data object OnRefundConfirmed : WooPosRefundUIEvent()
    data object RefundFlowDismissed : WooPosRefundUIEvent()
    data object RetryLoadRefundableItems : WooPosRefundUIEvent()
    data object RetryCreateRefund : WooPosRefundUIEvent()
    data object CancelRefund : WooPosRefundUIEvent()
}
