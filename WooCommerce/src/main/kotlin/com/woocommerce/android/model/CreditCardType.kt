package com.woocommerce.android.model

enum class CreditCardType {
    VISA,
    MASTERCARD,
    AMERICAN_EXPRESS,
    DISCOVER,
    JCB,
    DINERS_CLUB,
    UNKNOWN;

    companion object {
        fun fromString(string: String): CreditCardType {
            return when (string.lowercase()) {
                "visa" -> VISA
                "mastercard" -> MASTERCARD
                "american_express" -> AMERICAN_EXPRESS
                "discover" -> DISCOVER
                "jcb" -> JCB
                "diners_club" -> DINERS_CLUB
                else -> UNKNOWN
            }
        }
    }
}
