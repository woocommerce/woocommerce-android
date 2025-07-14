package com.woocommerce.android.wear.ui.stats.datasource

import com.woocommerce.android.wear.ui.stats.datasource.StoreStatsData.StatRequest
import com.woocommerce.android.wear.ui.stats.datasource.StoreStatsData.StatRequest.Waiting
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StoreStatsDataTest {

    @Test
    fun `StoreStatsData with finished requests should return correct values`() {
        val revenueData = StoreStatsData.RevenueData(totalRevenue = "1000", orderCount = 10)
        val visitorData = 100

        val storeStatsData = StoreStatsData(revenueData, visitorData)

        assertThat(storeStatsData.revenue).isEqualTo("1000")
        assertThat(storeStatsData.ordersCount).isEqualTo(10)
        assertThat(storeStatsData.visitorsCount).isEqualTo(100)
        assertThat(storeStatsData.conversionRate).isEqualTo("10%")
        assertThat(storeStatsData.isComplete).isTrue
    }

    @Test
    fun `StoreStatsData with waiting requests should return default values`() {
        val revenueRequest = Waiting<StoreStatsData.RevenueData>()
        val visitorRequest = Waiting<Int>()

        val storeStatsData = StoreStatsData(revenueRequest, visitorRequest)

        assertThat(storeStatsData.revenue).isEqualTo("")
        assertThat(storeStatsData.ordersCount).isEqualTo(0)
        assertThat(storeStatsData.visitorsCount).isEqualTo(0)
        assertThat(storeStatsData.conversionRate).isEqualTo("0%")
        assertThat(storeStatsData.isComplete).isFalse
    }

    @Test
    fun `StoreStatsData with error requests should return default values`() {
        val revenueRequest = StatRequest.Error<StoreStatsData.RevenueData>()
        val visitorRequest = StatRequest.Error<Int>()

        val storeStatsData = StoreStatsData(revenueRequest, visitorRequest)

        assertThat(storeStatsData.revenue).isEqualTo("")
        assertThat(storeStatsData.ordersCount).isEqualTo(0)
        assertThat(storeStatsData.visitorsCount).isEqualTo(0)
        assertThat(storeStatsData.conversionRate).isEqualTo("0%")
        assertThat(storeStatsData.isComplete).isFalse
    }

    @Test
    fun `StoreStatsData with mixed finished and waiting requests should return incomplete`() {
        val revenueData = StoreStatsData.RevenueData(totalRevenue = "1000", orderCount = 10)
        val visitorRequest = Waiting<Int>()

        val storeStatsData = StoreStatsData(StatRequest.Finished(revenueData), visitorRequest)

        assertThat(storeStatsData.isComplete).isFalse
    }

    @Test
    fun `StoreStatsData with mixed finished and error requests should return complete`() {
        val revenueData = StoreStatsData.RevenueData(totalRevenue = "1000", orderCount = 10)
        val visitorRequest = StatRequest.Error<Int>()

        val storeStatsData = StoreStatsData(StatRequest.Finished(revenueData), visitorRequest)

        assertThat(storeStatsData.isComplete).isTrue
    }

    @Test
    fun `StoreStatsData with mixed waiting and error requests should return complete`() {
        val revenueRequest = Waiting<StoreStatsData.RevenueData>()
        val visitorRequest = StatRequest.Error<Int>()

        val storeStatsData = StoreStatsData(revenueRequest, visitorRequest)

        assertThat(storeStatsData.isComplete).isFalse
    }
}
