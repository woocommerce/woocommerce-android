package com.woocommerce.android.ui.woopos.cardreader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosCardReaderPaymentStatus : Parcelable {
    data object Success : WooPosCardReaderPaymentStatus()
    data object Failure : WooPosCardReaderPaymentStatus()
    data object Started : WooPosCardReaderPaymentStatus()
}
