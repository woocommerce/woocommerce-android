package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.woopos.products.GetTotalProductCount
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemsNextPageLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.PreSearchRecentTermTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchRemoteResultsFetched
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.IS_SEARCH
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ITEM_LIST_TYPE
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ITEM_LIST_TYPE_PRODUCTS
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import javax.inject.Inject

class WooPosItemsSearchAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val getTotalProductCount: GetTotalProductCount
) {
    suspend fun trackItemsNextPageLoaded() {
        val event = ItemsNextPageLoaded.apply {
            addProperties(
                mapOf(
                    ITEM_LIST_TYPE to ITEM_LIST_TYPE_PRODUCTS,
                    IS_SEARCH to "true"
                )
            )
        }
        analyticsTracker.track(event)
    }

    suspend fun trackRecentSearchSelected() {
        val event = PreSearchRecentTermTapped.apply {
            addProperties(
                mapOf(ITEM_LIST_TYPE to ITEM_LIST_TYPE_PRODUCTS)
            )
        }
        analyticsTracker.track(event)
    }

    suspend fun trackSearchPerformance(searchTimeMillis: Long) {
        val totalProductsCount = getTotalProductCount()
        val event = SearchRemoteResultsFetched(
            totalProductsCount = totalProductsCount,
            millisecondsSinceRequestSent = searchTimeMillis
        )
        analyticsTracker.track(event)
    }
}
