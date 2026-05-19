package com.woocommerce.android.ui.woopos.markorderascomplete

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

const val MARK_ORDER_AS_COMPLETE_ROUTE_ORDER_ID_KEY = "orderId"

@Parcelize
sealed class WooPosMarkOrderAsCompleteState : Parcelable {
    @Parcelize
    data object Initiating : WooPosMarkOrderAsCompleteState()

    @Parcelize
    data class Confirming(
        val totalText: String,
        val note: String,
        val errorMessage: String?,
        val button: Button,
    ) : WooPosMarkOrderAsCompleteState() {
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
