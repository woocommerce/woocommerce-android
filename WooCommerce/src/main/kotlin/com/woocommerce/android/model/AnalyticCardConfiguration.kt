package com.woocommerce.android.model

import com.woocommerce.android.R

data class AnalyticCardConfiguration(
    val id: Int,
    val title: String,
    val isVisible: Boolean = true,
)

enum class AnalyticsCards(val resId: Int) {
    Revenue(R.string.analytics_revenue_card_title),
    Orders(R.string.analytics_orders_card_title),
    Products(R.string.analytics_products_card_title),
    Session(R.string.analytics_session_card_title)
}
