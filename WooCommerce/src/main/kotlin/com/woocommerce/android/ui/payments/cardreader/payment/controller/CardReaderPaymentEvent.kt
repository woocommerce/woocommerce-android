package com.woocommerce.android.ui.payments.cardreader.payment.controller

import androidx.annotation.StringRes

sealed class CardReaderPaymentEvent {

    data class ShowPaymentErrorMessage(@StringRes val message: Int) : CardReaderPaymentEvent()

    data class ShowErrorMessage(@StringRes val message: Int) : CardReaderPaymentEvent()

    data object PlaySuccessfulPaymentSound : CardReaderPaymentEvent()

    data object InteracRefundSuccessful : CardReaderPaymentEvent()

    data object ContactSupportTapped : CardReaderPaymentEvent()

    data object EnableNfcTapped : CardReaderPaymentEvent()

    data class PurchaseCardReaderTapped(val url: String) : CardReaderPaymentEvent()

    data class PrintReceiptTapped(val receiptUrl: String, val documentName: String) : CardReaderPaymentEvent()

    data object Exit : CardReaderPaymentEvent()
}
