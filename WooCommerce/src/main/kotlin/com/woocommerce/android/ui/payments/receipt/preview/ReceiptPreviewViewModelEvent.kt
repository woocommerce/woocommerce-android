package com.woocommerce.android.ui.payments.receipt.preview

import com.woocommerce.android.viewmodel.MultiLiveEvent

data class LoadUrl(val url: String) : MultiLiveEvent.Event()

data class PrintReceipt(val receiptUrl: String, val documentName: String) : MultiLiveEvent.Event()
