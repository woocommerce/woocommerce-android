package com.woocommerce.android.model

import java.text.DecimalFormat

data class SessionStat(
    val ordersCount: Int,
    val visitorsCount: Int
) {
    val conversionRate: String
        get() = when {
            visitorsCount > 0 -> (ordersCount / visitorsCount.toFloat()) * 100
            else -> 0F
        }.let { DecimalFormat("##.#").format(conversionRate) + "%" }

    companion object {
        val EMPTY = SessionStat(
            ordersCount = 0,
            visitorsCount = 0
        )
    }
}
