package com.woocommerce.android.ui.woopos.emailreceipt

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosEmailReceiptState : Parcelable {
    @Parcelize
    data class Email(
        val email: String,
        val errorMessage: String?,
        val button: Button
    ) : WooPosEmailReceiptState() {
        @Parcelize
        data class Button(
            val text: String,
            val status: Status,
        ) : Parcelable {
            @Parcelize
            enum class Status : Parcelable {
                ENABLED,
                DISABLED,
                LOADING
            }
        }
    }

    @Parcelize
    object Sent : WooPosEmailReceiptState()
}
