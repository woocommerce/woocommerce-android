package com.woocommerce.android.ui.cardreader.receipt

import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

sealed class ReceiptEvent : Event() {
    data class PrintReceipt(val receiptUrl: String, val documentName: String) : ReceiptEvent()
    data class SendReceipt(val content: UiString, val subject: UiString, val address: String) : ReceiptEvent()
}
