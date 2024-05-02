package com.woocommerce.android.model

import com.woocommerce.android.R

data class AnalyticCardConfiguration(
    val card: AnalyticsCards,
    val title: String,
    val isVisible: Boolean = true,
)

enum class AnalyticsCards(val resId: Int, val isPlugin: Boolean = false) {
    Revenue(R.string.analytics_revenue_card_title),
    Orders(R.string.analytics_orders_card_title),
    Products(R.string.analytics_products_card_title),
    Session(R.string.analytics_session_card_title, isPlugin = true),
    Bundles(R.string.analytics_bundles_card_title, isPlugin = true),
    GiftCards(R.string.analytics_gift_cards_card_title, isPlugin = true)
}
