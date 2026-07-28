package org.wordpress.android.fluxc.endpoints

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class WCWPAPIEndpointTest {
    @Test
    fun testAllEndpoints() {
        // Orders
        assertEquals("/orders/", WOOCOMMERCE.orders.endpoint)
    }

    @Test
    fun testAllUrls() {
        // Orders
        assertEquals("/wc/v3/orders/", WOOCOMMERCE.orders.pathV3)
    }

    @Test
    fun testRevenueStatsUrl() {
        assertEquals("/wc-analytics/reports/revenue/stats/", WOOCOMMERCE.reports.revenue.stats.pathV4Analytics)
    }

    @Test
    fun `when building v4 refund paths, then namespace and routes are correct`() {
        assertEquals("/wc/v4/refunds/", WOOCOMMERCE.refunds.pathV4)
        assertEquals("/wc/v4/refunds/preview/", WOOCOMMERCE.refunds.preview.pathV4)
    }

    @Test
    fun `when building product duplicate path, then v3 route is correct`() {
        assertEquals("/wc/v3/products/123/duplicate/", WOOCOMMERCE.products.id(123).duplicate.pathV3)
    }
}
