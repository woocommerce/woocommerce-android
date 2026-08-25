package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrderDetailsEmailReceiptTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrderDetailsLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListFetched
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListNextPageLoaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListPullToRefreshTriggered
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListRowTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListSearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.OrdersListSearchResultsFetched
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import org.apache.commons.lang3.time.DateUtils.MILLIS_PER_DAY
import javax.inject.Inject

class WooPosOrdersAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker
) {
    suspend fun trackOrdersListLoaded() {
        analyticsTracker.track(OrdersListLoaded)
    }

    suspend fun trackOrdersListFetched(elapsedMs: Long) {
        analyticsTracker.track(OrdersListFetched(elapsedMs))
    }

    suspend fun trackOrdersListRowTapped(
        orderId: Long,
        orderStatus: String,
        listPosition: Int,
        createdAtMillis: Long
    ) {
        val daysSinceCreated = calculateDaysSinceCreated(createdAtMillis)
        analyticsTracker.track(
            OrdersListRowTapped(
                orderId = orderId,
                orderStatus = orderStatus,
                listPosition = listPosition,
                daysSinceCreated = daysSinceCreated
            )
        )
    }

    suspend fun trackOrderDetailsLoaded(
        orderId: Long,
        orderStatus: String,
        createdAtMillis: Long
    ) {
        val daysSinceCreated = calculateDaysSinceCreated(createdAtMillis)
        analyticsTracker.track(
            OrderDetailsLoaded(
                orderId = orderId,
                orderStatus = orderStatus,
                daysSinceCreated = daysSinceCreated
            )
        )
    }

    suspend fun trackOrdersListPullToRefreshTriggered() {
        analyticsTracker.track(OrdersListPullToRefreshTriggered)
    }

    suspend fun trackOrderDetailsEmailReceiptTapped() {
        analyticsTracker.track(OrderDetailsEmailReceiptTapped)
    }

    suspend fun trackOrdersListNextPageLoaded() {
        analyticsTracker.track(OrdersListNextPageLoaded)
    }

    suspend fun trackOrdersListSearchButtonTapped() {
        analyticsTracker.track(OrdersListSearchButtonTapped)
    }

    suspend fun trackOrdersListSearchResultsFetched(elapsedMs: Long) {
        analyticsTracker.track(OrdersListSearchResultsFetched(elapsedMs))
    }

    private fun calculateDaysSinceCreated(createdAtMillis: Long): Int {
        return ((System.currentTimeMillis() - createdAtMillis) / MILLIS_PER_DAY)
            .toInt()
            .coerceAtLeast(0)
    }
}
