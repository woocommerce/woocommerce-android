package com.woocommerce.android.model

data class SessionStat(
    val conversionRate: String,
    val viewsCount: Int
) {
    companion object {
        val EMPTY = SessionStat(
            conversionRate = "",
            viewsCount = 0
        )
    }
}
