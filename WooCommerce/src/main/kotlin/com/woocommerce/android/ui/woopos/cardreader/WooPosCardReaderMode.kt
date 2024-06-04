package com.woocommerce.android.ui.woopos.cardreader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class WooPosCardReaderMode : Parcelable {
    data object Connection : WooPosCardReaderMode()
    data class Payment(val orderId: Long) : WooPosCardReaderMode()
}
