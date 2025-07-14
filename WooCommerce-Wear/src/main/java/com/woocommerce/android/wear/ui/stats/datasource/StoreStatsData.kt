package com.woocommerce.android.wear.ui.stats.datasource

import com.woocommerce.android.wear.extensions.convertedFrom

class StoreStatsData(
    private val revenueRequest: StatRequest<RevenueData>,
    private val visitorRequest: StatRequest<Int>
) {
    private val revenueData: RevenueData?
    private val visitorData: Int?

    constructor(
        revenueData: RevenueData,
        visitorData: Int
    ) : this(StatRequest.Finished(revenueData), StatRequest.Finished(visitorData))

    init {
        revenueData = when (revenueRequest) {
            is StatRequest.Finished -> revenueRequest.data
            else -> null
        }

        visitorData = when (visitorRequest) {
            is StatRequest.Finished -> visitorRequest.data
            else -> null
        }
    }

    val revenue get() = revenueData?.totalRevenue.orEmpty()
    val ordersCount get() = revenueData?.orderCount ?: 0
    val visitorsCount get() = visitorData ?: 0
    val conversionRate: String
        get() {
            val ordersCount = revenueData?.orderCount ?: 0
            val visitorsCount = visitorData ?: 0
            return ordersCount convertedFrom visitorsCount
        }

    val isComplete
        get() = revenueRequest is StatRequest.Finished && visitorRequest.isComplete

    data class RevenueData(
        val totalRevenue: String,
        val orderCount: Int
    )

    sealed class StatRequest<T> {
        class Error<T> : StatRequest<T>()
        class Waiting<T> : StatRequest<T>()
        class Finished<T>(val data: T) : StatRequest<T>()

        val isComplete get() = this is Finished || this is Error
    }
}
