package com.woocommerce.android.model

import com.woocommerce.android.extensions.limitTo
import java.text.DecimalFormat

data class SessionStat(
    val ordersCount: Int,
    val visitorsCount: Int
) {
    val conversionRate: String
        get() = when {
            visitorsCount > 0 -> (ordersCount / visitorsCount.toDouble()) * PERCENT_BASE
            else -> 0.0
        }.limitTo(100.0).let { DecimalFormat("##.#").format(it) + "%" }


    companion object {
        val EMPTY = SessionStat(
            ordersCount = 0,
            visitorsCount = 0
        )
        const val PERCENT_BASE = 100
    }
}
