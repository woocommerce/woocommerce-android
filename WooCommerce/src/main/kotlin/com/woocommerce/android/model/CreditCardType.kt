package com.woocommerce.android.model

import androidx.annotation.DrawableRes
import com.woocommerce.android.R

enum class CreditCardType {
    VISA,
    MASTERCARD,
    UNIONPAY,
    JCB,
    DISCOVER,
    DINERS,
    AMEX,
    UNKNOWN;

    val icon: Int
        @DrawableRes
        get() = when (this) {
            VISA -> R.drawable.credit_card_visa
            MASTERCARD -> R.drawable.credit_card_mastercard
            UNIONPAY -> R.drawable.credit_card_unionpay
            JCB -> R.drawable.credit_card_jcb
            DISCOVER -> R.drawable.credit_card_discover
            DINERS -> R.drawable.credit_card_diners
            AMEX -> R.drawable.credit_card_amex
            UNKNOWN -> R.drawable.credit_card_placeholder
        }

    companion object {
        fun fromString(string: String): CreditCardType {
            return when (string.lowercase()) {
                "visa" -> VISA
                "mastercard" -> MASTERCARD
                "unionpay" -> UNIONPAY
                "jcb" -> JCB
                "discover" -> DISCOVER
                "diners" -> DINERS
                "amex" -> AMEX
                else -> UNKNOWN
            }
        }
    }
}
