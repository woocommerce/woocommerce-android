package com.woocommerce.android.ui.woopos.markorderaspaid

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

const val MARK_ORDER_AS_PAID_ROUTE_ORDER_ID_KEY = "orderId"

@Parcelize
sealed class WooPosMarkOrderAsPaidState : Parcelable {
    @Parcelize
    data object Initiating : WooPosMarkOrderAsPaidState()

    @Parcelize
    data class Confirming(
        val formattedTotal: String,
        val note: String,
        val errorMessage: String?,
        val isProcessing: Boolean,
        val canConfirm: Boolean,
    ) : WooPosMarkOrderAsPaidState()
}
