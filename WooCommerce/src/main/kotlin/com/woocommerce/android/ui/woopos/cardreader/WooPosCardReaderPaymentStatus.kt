package com.woocommerce.android.ui.woopos.cardreader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosCardReaderPaymentStatus : Parcelable {
    data object ReadyToCollectPayment : WooPosCardReaderPaymentStatus()
    data object FailureToPrepareForPayment : WooPosCardReaderPaymentStatus()
    data object Unknown : WooPosCardReaderPaymentStatus()
}
