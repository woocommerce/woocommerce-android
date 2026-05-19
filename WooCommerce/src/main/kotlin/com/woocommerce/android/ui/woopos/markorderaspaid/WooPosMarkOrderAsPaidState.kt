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
        val totalText: String,
        val note: String,
        val errorMessage: String?,
        val button: Button,
    ) : WooPosMarkOrderAsPaidState() {
        @Parcelize
        data class Button(
            val text: String,
            val status: Status,
        ) : Parcelable {
            @Parcelize
            enum class Status : Parcelable {
                ENABLED,
                LOADING,
                DISABLED,
            }
        }
    }
}
