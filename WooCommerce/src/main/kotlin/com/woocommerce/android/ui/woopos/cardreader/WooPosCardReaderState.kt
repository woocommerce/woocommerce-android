package com.woocommerce.android.ui.woopos.cardreader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
enum class WooPosCardReaderState: Parcelable {
    ReadyForPayment, NotReadyForPayment
}
