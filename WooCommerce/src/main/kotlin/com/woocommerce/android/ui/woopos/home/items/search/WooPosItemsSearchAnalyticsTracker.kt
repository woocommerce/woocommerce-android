package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemsNextPageLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.PreSearchRecentTermTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchRemoteResultsFetched
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchResultTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchResultsFetched
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosGetTotalProductCount
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class WooPosItemsSearchAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val getTotalProductCount: WooPosGetTotalProductCount,
) {
    private val localSearchProductIds = AtomicReference<List<Long>>(emptyList())

    suspend fun trackItemsNextPageLoaded() {
        val event = ItemsNextPageLoaded(
            source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT,
        )

        analyticsTracker.track(event)
    }

    suspend fun trackRecentSearchSelected() {
        val event = PreSearchRecentTermTapped(source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT)
        analyticsTracker.track(event)
    }

    suspend fun trackSearchPerformance(searchTimeMillis: Long) {
        val totalProductsCount = getTotalProductCount()
        val event = SearchRemoteResultsFetched(
            totalItemsCount = totalProductsCount,
            millisecondsSinceRequestSent = searchTimeMillis,
            source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
        )
        analyticsTracker.track(event)
    }

    suspend fun trackLocalSearchResults(
        resultsCount: Int,
        searchTimeMillis: Long,
        searchMethod: String
    ) {
        val event = SearchResultsFetched(
            millisecondsSinceRequestSent = searchTimeMillis,
            resultsCount = resultsCount,
            source = "purchasable_items",
            searchMethod = searchMethod,
        )
        analyticsTracker.track(event)
    }

    suspend fun trackSearchResultTapped(resultPosition: Int, resultType: String) {
        val event = SearchResultTapped(
            resultPosition = resultPosition,
            resultType = resultType,
        )
        analyticsTracker.track(event)
    }

    fun isProductInTheLocalSearchResult(productId: Long): Boolean = localSearchProductIds.get().contains(productId)

    fun storedLocalSearchResultIds(ids: List<Long>) {
        localSearchProductIds.set(ids)
    }
}
