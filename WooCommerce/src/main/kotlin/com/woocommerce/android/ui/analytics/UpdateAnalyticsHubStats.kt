package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.AnalyticsHubUpdateState.Finished
import com.woocommerce.android.ui.analytics.AnalyticsHubUpdateState.Loading
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection
import com.woocommerce.android.util.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdateAnalyticsHubStats @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val analyticsRepository: AnalyticsRepository
) {
    private val _revenueState = MutableStateFlow(RevenueState.Available(RevenueStat.EMPTY) as RevenueState)
    val revenueState: StateFlow<RevenueState> = _revenueState

    private val _productsState = MutableStateFlow(ProductsState.Available(ProductsStat.EMPTY) as ProductsState)
    val productsState: StateFlow<ProductsState> = _productsState

    private val _ordersState = MutableStateFlow(OrdersState.Available(OrdersStat.EMPTY) as OrdersState)
    val ordersState: StateFlow<OrdersState> = _ordersState

    private val _sessionState = MutableStateFlow(VisitorsState.Available(SessionStat.EMPTY) as VisitorsState)
    val sessionState: StateFlow<VisitorsState> = _sessionState

    private val visitorsCountState = MutableStateFlow(0)
    private val sessionChanges by lazy { combineSessionDataChanges() }

    suspend operator fun invoke(
        coroutineScope: CoroutineScope,
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ): Flow<AnalyticsHubUpdateState> {
        _ordersState.update { OrdersState.Loading }
        _sessionState.update { VisitorsState.Loading }
        _revenueState.update { RevenueState.Loading }
        _productsState.update { ProductsState.Loading }

        coroutineScope.launch(dispatchers.computation) {
            sessionChanges.collect { _sessionState.value = it }
        }

        fetchOrdersData(rangeSelection, fetchStrategy)
        fetchVisitorsCount(rangeSelection, fetchStrategy)
        fetchRevenueData(rangeSelection, fetchStrategy)
        fetchProductsData(rangeSelection, fetchStrategy)

        return combineStatesProgress()
    }

    private fun combineStatesProgress() =
        combine(_revenueState, _productsState, _ordersState, _sessionState) { revenue, products, orders, session ->
            revenue.isIdle && products.isIdle && orders.isIdle && session.isIdle
        }.map { if (it) Finished else Loading }

    private fun combineSessionDataChanges() =
        combine(_ordersState, visitorsCountState) { orders, visitorsCount ->
            orders.run { this as? OrdersState.Available }
                ?.orders?.ordersCount
                ?.let { VisitorsState.Available(SessionStat(it, visitorsCount)) }
                ?: when (orders) {
                    is OrdersState.Error -> VisitorsState.Error
                    else -> VisitorsState.Loading
                }
        }

    private suspend fun fetchOrdersData(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) {
        analyticsRepository.fetchOrdersData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.OrdersResult.OrdersData }
            ?.let { _ordersState.value = OrdersState.Available(it.ordersStat) }
            ?: _ordersState.update { OrdersState.Error }
    }

    private suspend fun fetchVisitorsCount(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) {
        analyticsRepository.fetchVisitorsData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.VisitorsResult.VisitorsData }
            .let { visitorsCountState.value = it?.visitorsCount ?: 0 }
    }

    private suspend fun fetchRevenueData(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) {
        analyticsRepository.fetchRevenueData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.RevenueResult.RevenueData }
            ?.let { _revenueState.value = RevenueState.Available(it.revenueStat) }
            ?: _revenueState.update { RevenueState.Error }
    }

    private suspend fun fetchProductsData(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) {
        analyticsRepository.fetchProductsData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.ProductsResult.ProductsData }
            ?.let { _productsState.value = ProductsState.Available(it.productsStat) }
            ?: _productsState.update { ProductsState.Error }
    }
}
