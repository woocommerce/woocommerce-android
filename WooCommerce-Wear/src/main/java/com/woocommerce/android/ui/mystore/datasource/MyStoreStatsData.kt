package com.woocommerce.android.ui.mystore.datasource

import com.woocommerce.commons.extensions.convertedFrom

data class RevenueData(
    val totalRevenue: Double,
    val orderCount: Int
)

sealed class MyStoreStatsRequest {
    data class Data(
        private val revenueData: RevenueData?,
        private val visitorData: Int?
    ) : MyStoreStatsRequest() {
        val isFinished
            get() = revenueData != null &&
                visitorData != null
        val revenue
            get() = revenueData?.totalRevenue ?: 0.0

        val ordersCount
            get() = revenueData?.orderCount ?: 0

        val visitorsCount
            get() = visitorData ?: 0

        val conversionRate: String
            get() {
                val ordersCount = revenueData?.orderCount ?: 0
                val visitorsCount = visitorData ?: 0
                return ordersCount convertedFrom visitorsCount
            }
    }

    data object Error : MyStoreStatsRequest()
}
