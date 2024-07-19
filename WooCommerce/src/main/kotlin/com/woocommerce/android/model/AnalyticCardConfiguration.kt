package com.woocommerce.android.model

import com.woocommerce.android.R

data class AnalyticCardConfiguration(
    val card: AnalyticsCards,
    val title: String,
    val isVisible: Boolean = true,
)

enum class AnalyticsCards(
    val resId: Int,
    val isPlugin: Boolean = false
) {
    Revenue(R.string.analytics_revenue_card_title),
    Orders(R.string.analytics_orders_card_title),
    Products(R.string.analytics_products_card_title),
    Session(R.string.analytics_session_card_title, isPlugin = true),
    Bundles(R.string.analytics_bundles_card_title, isPlugin = true),
    GiftCards(R.string.analytics_gift_cards_card_title, isPlugin = true),
    GoogleAds(R.string.analytics_google_ads_card_title, isPlugin = true);

    /**
     * Changing the AnalyticsCard name can cause crashes due to the attachment of the name with the Data Store.
     * To allow us to update the tracked name without causing issues to the Hub settings storage,
     * this field separates the AnalyticsCard definition from the tracked information.
     */
    val trackName: String
        get() = when (this) {
            Revenue -> "revenue"
            Orders -> "orders"
            Products -> "products"
            Session -> "session"
            Bundles -> "bundles"
            GiftCards -> "giftcards"
            GoogleAds -> "googleCampaigns"
        }
}
