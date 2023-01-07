package com.woocommerce.android.model

data class SessionStats(
    val visitorsCount: Int,
    val viewsCount: Int
) {
    companion object {
        val EMPTY = SessionStats(
            visitorsCount = 0,
            viewsCount = 0
        )
    }
}
