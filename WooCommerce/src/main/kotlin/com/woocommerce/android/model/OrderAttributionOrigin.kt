package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.OrderAttributionInfo

sealed interface OrderAttributionOrigin {
    data class Utm(val source: String?) : OrderAttributionOrigin
    data class Referral(val source: String?) : OrderAttributionOrigin
    data class Organic(val source: String?) : OrderAttributionOrigin
    data object Direct : OrderAttributionOrigin
    data object Admin : OrderAttributionOrigin
    data object Mobile : OrderAttributionOrigin {
        const val SOURCE_TYPE_VALUE = "mobile_app"
    }
    data object Unknown : OrderAttributionOrigin
}

val OrderAttributionInfo.origin
    get() = when (sourceType) {
        "utm" -> OrderAttributionOrigin.Utm(source)
        "referral" -> OrderAttributionOrigin.Referral(source)
        "organic" -> OrderAttributionOrigin.Organic(source)
        "typein" -> OrderAttributionOrigin.Direct
        "admin" -> OrderAttributionOrigin.Admin
        OrderAttributionOrigin.Mobile.SOURCE_TYPE_VALUE -> OrderAttributionOrigin.Mobile
        else -> OrderAttributionOrigin.Unknown
    }
