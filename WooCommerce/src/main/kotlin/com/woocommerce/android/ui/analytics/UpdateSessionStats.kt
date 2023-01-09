package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy
import com.woocommerce.android.ui.analytics.AnalyticsViewModel.OrdersState
import com.woocommerce.android.ui.analytics.AnalyticsViewModel.VisitorsState
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection
import com.woocommerce.android.util.CoroutineDispatchers
import java.text.DecimalFormat
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update

class UpdateSessionStats @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val analyticsRepository: AnalyticsRepository
) {
    private val ordersState = MutableStateFlow(OrdersState.Available(OrdersStat.EMPTY) as OrdersState)
    private val visitorsCountState = MutableStateFlow(0)

    private val sessionChanges: Flow<VisitorsState>

    init {
        sessionChanges = combine(ordersState, visitorsCountState) { orders, visitorsCount ->
            orders.run { this as? OrdersState.Available }
                ?.orders?.ordersCount
                ?.let { (it / visitorsCount.toFloat()) * 100 }
                ?.let { DecimalFormat("##.#").format(it) + "%" }
                ?.let { SessionStat(it, visitorsCount) }
                ?.let { VisitorsState.Available(it) }
                ?: when (orders) {
                    is OrdersState.Error -> VisitorsState.Error
                    else -> VisitorsState.Loading
                }
        }
    }

    suspend operator fun invoke(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) : Flow<VisitorsState> {
        ordersState.update { OrdersState.Loading }

        fetchOrdersData(rangeSelection, fetchStrategy)
            .flowOn(dispatchers.io)
            .collect { ordersState.update { it } }

        fetchVisitorsCount(rangeSelection, fetchStrategy)
            .flowOn(dispatchers.io)
            .collect { visitorsCountState.update { it } }

        return sessionChanges
    }

    private fun fetchOrdersData(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) = flow {
        analyticsRepository.fetchOrdersData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.OrdersResult.OrdersData }
            ?.let { emit(OrdersState.Available(it.ordersStat)) }
            ?: emit(OrdersState.Error)
    }

    private fun fetchVisitorsCount(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) = flow {
        analyticsRepository.fetchVisitorsData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.VisitorsResult.VisitorsData }
            .let { emit(it?.visitorsCount ?: 0) }
    }
}
