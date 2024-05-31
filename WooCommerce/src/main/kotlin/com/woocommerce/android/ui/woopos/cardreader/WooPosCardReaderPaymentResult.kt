package com.woocommerce.android.ui.woopos.cardreader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosCardReaderPaymentResult : Parcelable {
    data object Success : WooPosCardReaderPaymentResult()
    data object Failure : WooPosCardReaderPaymentResult()
}
