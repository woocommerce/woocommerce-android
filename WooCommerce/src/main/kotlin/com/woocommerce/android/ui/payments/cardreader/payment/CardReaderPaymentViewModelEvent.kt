package com.woocommerce.android.ui.payments.cardreader.payment

import androidx.annotation.StringRes
import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

class ShowSnackbarInDialog(@StringRes val message: Int) : Event()

object PlayChaChing : Event()

object InteracRefundSuccessful : Event()

object ContactSupport : Event()

object EnableNfc : Event()

data class PurchaseCardReader(val url: String) : Event()

data class PrintReceipt(val receiptUrl: String, val documentName: String) : Event()

data class SendReceipt(val content: UiString, val subject: UiString, val address: String) : Event()
