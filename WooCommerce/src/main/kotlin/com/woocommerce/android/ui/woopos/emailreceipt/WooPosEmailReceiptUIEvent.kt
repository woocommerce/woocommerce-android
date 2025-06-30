package com.woocommerce.android.ui.woopos.emailreceipt

sealed class WooPosEmailReceiptUIEvent {
    object SendEmailClicked : WooPosEmailReceiptUIEvent()
    data class EmailChanged(val email: String) : WooPosEmailReceiptUIEvent()
}
