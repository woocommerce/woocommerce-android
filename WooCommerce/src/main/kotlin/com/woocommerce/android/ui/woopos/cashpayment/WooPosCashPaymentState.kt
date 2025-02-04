package com.woocommerce.android.ui.woopos.cashpayment

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCSettingsModel
import java.math.BigDecimal

@Parcelize
sealed class WooPosCashPaymentState : Parcelable {
    data class Collecting(
        val enteredAmount: BigDecimal?,
        val errorMessage: String?,
        val changeDueText: String,
        val total: BigDecimal,
        val totalText: String,
        val currencySymbol: String,
        val currencyPosition: WCSettingsModel.CurrencyPosition,
        val decimalSeparator: String,
        val numberOfDecimals: Int,
        val button: Button
    ) : WooPosCashPaymentState() {
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

    object Complete : WooPosCashPaymentState()

    object Initiating : WooPosCashPaymentState()
}
