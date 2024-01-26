package com.woocommerce.android.ui.payments.receipt

import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.MultiLiveEvent

data class LoadUrl(val url: String) : MultiLiveEvent.Event()

data class PrintReceipt(val receiptUrl: String, val documentName: String) : MultiLiveEvent.Event()

data class SendReceipt(val content: UiString, val subject: UiString, val address: String) : MultiLiveEvent.Event()
