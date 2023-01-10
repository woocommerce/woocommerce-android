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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdateAnalyticsHubStats @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val analyticsRepository: AnalyticsRepository
) {
    val revenueState = MutableStateFlow(RevenueState.Available(RevenueStat.EMPTY) as RevenueState)
    val productsState = MutableStateFlow(ProductsState.Available(ProductsStat.EMPTY) as ProductsState)
    val ordersState = MutableStateFlow(OrdersState.Available(OrdersStat.EMPTY) as OrdersState)
    val sessionState = MutableStateFlow(VisitorsState.Available(SessionStat.EMPTY) as VisitorsState)

    private val visitorsCountState = MutableStateFlow(0)
    private val sessionChanges: Flow<VisitorsState> by lazy { combineSessionDataChanges() }

    operator fun invoke(
        coroutineScope: CoroutineScope,
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy,
        loadWithSkeleton: Boolean
    ) = coroutineScope.launch {
        ordersState.update { OrdersState.Loading(loadWithSkeleton) }
        sessionState.update { VisitorsState.Loading(loadWithSkeleton) }
        revenueState.update { RevenueState.Loading(loadWithSkeleton) }
        productsState.update { ProductsState.Loading(loadWithSkeleton) }


        coroutineScope.launch(dispatchers.computation) {
            sessionChanges.collect { sessionState.value = it }
        }

        fetchOrdersData(rangeSelection, fetchStrategy)
        fetchVisitorsCount(rangeSelection, fetchStrategy)
        fetchRevenueData(rangeSelection, fetchStrategy)
        fetchProductsData(rangeSelection, fetchStrategy)
    }

    private suspend fun fetchOrdersData(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) {
        analyticsRepository.fetchOrdersData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.OrdersResult.OrdersData }
            ?.let { ordersState.value = OrdersState.Available(it.ordersStat) }
            ?: ordersState.update { OrdersState.Error }
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
            ?.let { revenueState.value = RevenueState.Available(it.revenueStat) }
            ?: revenueState.update { RevenueState.Error }
    }

    private suspend fun fetchProductsData(
        rangeSelection: AnalyticsHubDateRangeSelection,
        fetchStrategy: FetchStrategy
    ) {
        analyticsRepository.fetchProductsData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.ProductsResult.ProductsData }
            ?.let { productsState.value = ProductsState.Available(it.productsStat) }
            ?: productsState.update { ProductsState.Error }
    }

    private fun combineSessionDataChanges() =
        combine(ordersState, visitorsCountState) { orders, visitorsCount ->
            orders.run { this as? OrdersState.Available }
                ?.orders?.ordersCount
                ?.let { generateVisitorState(it, visitorsCount) }
                ?: when (orders) {
                    is OrdersState.Error -> VisitorsState.Error
                    else -> VisitorsState.Loading(true)
                }
        }

    private fun generateVisitorState(ordersCount: Int, visitorsCount: Int): VisitorsState {
        val conversionRate = (ordersCount / visitorsCount.toFloat()) * 100
        val formattedConversionRate = DecimalFormat("##.#").format(conversionRate) + "%"
        return VisitorsState.Available(SessionStat(formattedConversionRate, visitorsCount))
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
        data class Available(val products: ProductsStat) : ProductsState()
        data class Loading(val withSkeleton: Boolean) : ProductsState()
        object Error : ProductsState()
    }
}
