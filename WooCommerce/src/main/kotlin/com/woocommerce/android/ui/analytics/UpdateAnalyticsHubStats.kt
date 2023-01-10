package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.AnalyticsHubUpdateState.Finished
import com.woocommerce.android.ui.analytics.AnalyticsHubUpdateState.Loading
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class UpdateAnalyticsHubStats @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    private val _revenueState = MutableStateFlow(RevenueState.Available(RevenueStat.EMPTY) as RevenueState)
    val revenueState: Flow<RevenueState> = _revenueState

    private val _productsState = MutableStateFlow(ProductsState.Available(ProductsStat.EMPTY) as ProductsState)
    val productsState: Flow<ProductsState> = _productsState

    private val _ordersState = MutableStateFlow(OrdersState.Available(OrdersStat.EMPTY) as OrdersState)
    val ordersState: Flow<OrdersState> = _ordersState

    private val visitorsCountState = MutableStateFlow(VisitorsState.Available(0) as VisitorsState)
    val sessionState by lazy { combineSessionDataChanges() }


    suspend operator fun invoke(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ): Flow<AnalyticsHubUpdateState> {
        _ordersState.update { OrdersState.Loading }
        _revenueState.update { RevenueState.Loading }
        _productsState.update { ProductsState.Loading }
        visitorsCountState.update { VisitorsState.Loading }

        fetchOrdersData(rangeSelection, fetchStrategy)
        fetchVisitorsCount(rangeSelection, fetchStrategy)
        fetchRevenueData(rangeSelection, fetchStrategy)
        fetchProductsData(rangeSelection, fetchStrategy)

        return combineFullUpdateState()
    }

    private fun combineFullUpdateState() =
        combine(_revenueState, _productsState, _ordersState, sessionState) { revenue, products, orders, session ->
            revenue.isIdle && products.isIdle && orders.isIdle && session.isIdle
        }.map { if (it) Finished else Loading }

    private fun combineSessionDataChanges() =
        combine(_ordersState, visitorsCountState) { orders, visitors ->
            if (orders is OrdersState.Available && visitors is VisitorsState.Available) {
                SessionState.Available(SessionStat(orders.orders.ordersCount, visitors.visitors))
            } else if (orders is OrdersState.Error || visitors is VisitorsState.Error) {
                SessionState.Error
            } else {
                SessionState.Loading
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
            ?.let { visitorsCountState.value = VisitorsState.Available(it.visitorsCount) }
            ?: visitorsCountState.update { VisitorsState.Error }
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
