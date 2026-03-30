package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.data.OrderAttributionRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetSalesByChannel @Inject constructor(
    private val orderAttributionRepository: OrderAttributionRepository,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    operator fun invoke(
        selectedRange: StatsTimeRangeSelection,
        forceRefresh: Boolean = false
    ): Flow<SalesByChannelResult> = flow {
        emit(SalesByChannelResult.Loading)

        val currentFrom = selectedRange.currentRange.start.formatToYYYYmmDDhhmmss()
        val currentTo = selectedRange.currentRange.end.formatToYYYYmmDDhhmmss()
        val compareFrom = selectedRange.previousRange.start.formatToYYYYmmDDhhmmss()
        val compareTo = selectedRange.previousRange.end.formatToYYYYmmDDhhmmss()

        orderAttributionRepository.fetchChannelSummary(
            from = currentFrom,
            to = currentTo,
            compareFrom = compareFrom,
            compareTo = compareTo
        ).fold(
            onSuccess = { apiResponses ->
                val channels = apiResponses.mapNotNull { response ->
                    val channelName = response.channelName ?: return@mapNotNull null
                    ChannelSales(
                        channelName = channelName,
                        revenue = response.netRevenue ?: 0.0,
                        compareRevenue = response.compareNetRevenue ?: 0.0,
                        ordersCount = response.ordersCount ?: 0,
                        compareOrdersCount = response.compareOrdersCount ?: 0
                    )
                }
                emit(SalesByChannelResult.Success(channels))
            },
            onFailure = { exception ->
                emit(SalesByChannelResult.Error(exception))
            }
        )
    }.flowOn(coroutineDispatchers.computation)

    data class ChannelSales(
        val channelName: String,
        val revenue: Double,
        val compareRevenue: Double,
        val ordersCount: Int,
        val compareOrdersCount: Int
    )

    sealed class SalesByChannelResult {
        data object Loading : SalesByChannelResult()
        data class Error(val exception: Throwable) : SalesByChannelResult()
        data class Success(val channels: List<ChannelSales>) : SalesByChannelResult()
    }
}
