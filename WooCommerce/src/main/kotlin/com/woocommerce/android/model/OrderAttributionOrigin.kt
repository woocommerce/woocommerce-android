package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.OrderAttributionInfo

sealed interface OrderAttributionOrigin {
    data class Utm(val source: String?) : OrderAttributionOrigin
    data class Referral(val source: String?) : OrderAttributionOrigin
    data class Organic(val source: String?) : OrderAttributionOrigin
    data object Direct : OrderAttributionOrigin
    data object Admin : OrderAttributionOrigin
    data object Mobile : OrderAttributionOrigin
    data object Unknown : OrderAttributionOrigin
}

val OrderAttributionInfo.origin
    get() = when (sourceType) {
        "utm" -> OrderAttributionOrigin.Utm(source)
        "referral" -> OrderAttributionOrigin.Referral(source)
        "organic" -> OrderAttributionOrigin.Organic(source)
        "typein" -> OrderAttributionOrigin.Direct
        "admin" -> OrderAttributionOrigin.Admin
        "mobile_app" -> OrderAttributionOrigin.Mobile
        else -> OrderAttributionOrigin.Unknown
    }
