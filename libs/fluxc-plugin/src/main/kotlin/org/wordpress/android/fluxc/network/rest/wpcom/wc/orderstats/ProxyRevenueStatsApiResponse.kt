package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.Response

/**
 * Response model for the WP.com analytics proxy endpoint:
 * `/wc/v3/woocommerce-analytics/proxy/reports/orders/by-date`
 *
 * This endpoint returns a different JSON structure than the local
 * `/wc-analytics/reports/revenue/stats` endpoint:
 * - `summary` (object with string values) instead of `totals` (object with numeric values)
 * - `data` (array with flat objects) instead of `intervals` (array with nested `subtotals`)
 *
 * Field names also differ:
 * - `orders_no` instead of `orders_count`
 * - `orders_value_net` instead of `net_revenue`
 * - `time_interval` instead of `interval`
 * - `average_order_value` instead of `avg_order_value`
 * - `total_items` instead of `num_items_sold`
 */
class ProxyRevenueStatsApiResponse : Response {
    val summary: JsonElement? = null
    val data: JsonElement? = null
}
