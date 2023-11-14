package com.woocommerce.android.model

import com.woocommerce.android.extensions.convertedFrom

data class SessionStat(
    val ordersCount: Int,
    val visitorsCount: Int
) {
    val conversionRate: String
        get() = ordersCount convertedFrom visitorsCount

    companion object {
        val EMPTY = SessionStat(
            ordersCount = 0,
            visitorsCount = 0
        )
    }
}
