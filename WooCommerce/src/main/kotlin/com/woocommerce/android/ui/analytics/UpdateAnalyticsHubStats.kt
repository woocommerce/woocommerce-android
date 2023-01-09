package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy
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

class UpdateAnalyticsHubStats @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val analyticsRepository: AnalyticsRepository
) {
    val revenueState = MutableStateFlow(RevenueState.Available(RevenueStat.EMPTY) as RevenueState)
    val productsState = MutableStateFlow(ProductsState.Available(ProductsStat.EMPTY) as ProductsState)
    val ordersState = MutableStateFlow(OrdersState.Available(OrdersStat.EMPTY) as OrdersState)
    val sessionState = MutableStateFlow(VisitorsState.Available(SessionStat.EMPTY) as VisitorsState)

    private val visitorsCountState = MutableStateFlow(0)
    private val sessionChanges: Flow<VisitorsState> =
        combine(ordersState, visitorsCountState) { orders, visitorsCount ->
            orders.run { this as? OrdersState.Available }
                ?.orders?.ordersCount
                ?.let { (it / visitorsCount.toFloat()) * 100 }
                ?.let { DecimalFormat("##.#").format(it) + "%" }
                ?.let { SessionStat(it, visitorsCount) }
                ?.let { VisitorsState.Available(it) }
                ?: when (orders) {
                    is OrdersState.Error -> VisitorsState.Error
                    else -> VisitorsState.Loading(true)
                }
        }

    suspend operator fun invoke(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy,
        loadWithSkeleton: Boolean
    ) {
        ordersState.update { OrdersState.Loading(loadWithSkeleton) }
        sessionState.update { VisitorsState.Loading(loadWithSkeleton) }
        revenueState.update { RevenueState.Loading(loadWithSkeleton) }
        productsState.update { ProductsState.Loading(loadWithSkeleton) }

        sessionChanges
            .flowOn(dispatchers.computation)
            .collect { sessionState.update { it } }

        fetchOrdersData(rangeSelection, fetchStrategy)
            .flowOn(dispatchers.io)
            .collect { ordersState.update { it } }

        fetchVisitorsCount(rangeSelection, fetchStrategy)
            .flowOn(dispatchers.io)
            .collect { visitorsCountState.update { it } }

        fetchRevenueData(rangeSelection, fetchStrategy)
            .flowOn(dispatchers.io)
            .collect { revenueState.update { it } }

        fetchProductsData(rangeSelection, fetchStrategy)
            .flowOn(dispatchers.io)
            .collect { productsState.update { it } }
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

    private fun fetchRevenueData(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) = flow {
        analyticsRepository.fetchRevenueData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.RevenueResult.RevenueData }
            ?.let { emit(RevenueState.Available(it.revenueStat)) }
            ?: emit(RevenueState.Error)
    }

    private fun fetchProductsData(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) = flow {
        analyticsRepository.fetchProductsData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.ProductsResult.ProductsData }
            ?.let { emit(ProductsState.Available(it.productsStat)) }
            ?: emit(ProductsState.Error)
    }

    sealed class OrdersState {
        data class Available(val orders: OrdersStat) : OrdersState()
        data class Loading(val withSkeleton: Boolean) : OrdersState()
        object Error : OrdersState()
    }

    sealed class VisitorsState {
        data class Available(val session: SessionStat) : VisitorsState()
        data class Loading(val withSkeleton: Boolean) : VisitorsState()
        object Error : VisitorsState()
    }

    sealed class RevenueState {
        data class Available(val revenue: RevenueStat) : RevenueState()
        data class Loading(val withSkeleton: Boolean) : RevenueState()
        object Error : RevenueState()
    }

    sealed class ProductsState {
        data class Available(val product: ProductsStat) : ProductsState()
        data class Loading(val withSkeleton: Boolean) : ProductsState()
        object Error : ProductsState()
    }
}
