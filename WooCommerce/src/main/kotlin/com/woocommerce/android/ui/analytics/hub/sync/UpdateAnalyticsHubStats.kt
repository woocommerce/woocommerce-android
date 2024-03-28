package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.model.BundleItem
import com.woocommerce.android.model.BundleStat
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsHubUpdateState.Finished
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsHubUpdateState.Loading
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.FetchStrategy
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class UpdateAnalyticsHubStats @Inject constructor(
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore,
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

    private val _bundlesState = MutableStateFlow(BundlesState.Available(BundleStat.EMPTY) as BundlesState)
    val bundlesState: Flow<BundlesState> = _bundlesState

    private val fullStatsRequestState by lazy { combineFullUpdateState() }

    suspend operator fun invoke(
        rangeSelection: StatsTimeRangeSelection,
        scope: CoroutineScope,
        forceUpdate: Boolean = false,
        visibleCards: List<AnalyticsCards> = AnalyticsCards.entries,
    ): Flow<AnalyticsHubUpdateState> {
        visibleCards.forEach { card ->
            when (card) {
                AnalyticsCards.Revenue -> _revenueState.update { RevenueState.Loading }
                AnalyticsCards.Orders -> _ordersState.update { OrdersState.Loading }
                AnalyticsCards.Products -> _productsState.update { ProductsState.Loading }
                AnalyticsCards.Session -> visitorsCountState.update { VisitorsState.Loading }
                AnalyticsCards.Bundles -> _bundlesState.update { BundlesState.Loading }
            }
        }

        withFetchStrategyFrom(rangeSelection, forceUpdate) { fetchStrategy ->
            updateStatsData(scope, rangeSelection, fetchStrategy, visibleCards)
        }

        return fullStatsRequestState
    }

    private suspend fun updateStatsData(
        scope: CoroutineScope,
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy,
        visibleCards: List<AnalyticsCards>
    ) {
        val asyncCalls = visibleCards.map { card ->
            when (card) {
                AnalyticsCards.Revenue -> scope.fetchRevenueDataAsync(rangeSelection, fetchStrategy)
                AnalyticsCards.Orders -> scope.fetchOrdersDataAsync(rangeSelection, fetchStrategy)
                AnalyticsCards.Products -> scope.fetchProductsDataAsync(rangeSelection, fetchStrategy)
                AnalyticsCards.Session -> scope.fetchVisitorsCountAsync(rangeSelection, fetchStrategy)
                AnalyticsCards.Bundles -> scope.fetchBundlesDataAsync(rangeSelection, fetchStrategy)
            }
        }

        asyncCalls.awaitAll()

        if (fetchStrategy == FetchStrategy.ForceNew) {
            storeLastAnalyticsUpdate(visibleCards, rangeSelection)
        }
    }

    private suspend fun storeLastAnalyticsUpdate(
        visibleCards: List<AnalyticsCards>,
        rangeSelection: StatsTimeRangeSelection
    ) {
        visibleCards.forEach { card ->
            when (card) {
                AnalyticsCards.Revenue -> analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection,
                    AnalyticsUpdateDataStore.AnalyticData.REVENUE
                )

                AnalyticsCards.Orders -> analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection,
                    AnalyticsUpdateDataStore.AnalyticData.ORDERS
                )

                AnalyticsCards.Products -> analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection,
                    AnalyticsUpdateDataStore.AnalyticData.TOP_PERFORMERS
                )

                AnalyticsCards.Session -> analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection,
                    AnalyticsUpdateDataStore.AnalyticData.VISITORS
                )

                AnalyticsCards.Bundles -> analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection,
                    AnalyticsUpdateDataStore.AnalyticData.BUNDLES
                )
            }
        }
    }

    private suspend fun withFetchStrategyFrom(
        rangeSelection: StatsTimeRangeSelection,
        forceUpdate: Boolean,
        action: suspend (FetchStrategy) -> Unit
    ) {
        if (forceUpdate) {
            action(FetchStrategy.ForceNew)
            return
        }

        analyticsUpdateDataStore
            .shouldUpdateAnalytics(rangeSelection)
            .map { if (it) FetchStrategy.ForceNew else FetchStrategy.Saved }
            .firstOrNull()
            ?.let { action(it) }
            ?: action(FetchStrategy.ForceNew)
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
        }.distinctUntilChanged()

    private fun CoroutineScope.fetchOrdersDataAsync(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ) = async {
        analyticsRepository.fetchOrdersData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.OrdersResult.OrdersData }
            ?.let { _ordersState.value = OrdersState.Available(it.ordersStat) }
            ?: _ordersState.update { OrdersState.Error }
    }

    private fun CoroutineScope.fetchVisitorsCountAsync(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ) = async {
        analyticsRepository.fetchVisitorsData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.VisitorsResult.VisitorsData }
            ?.let { visitorsCountState.value = VisitorsState.Available(it.visitorsCount) }
            ?: visitorsCountState.update { VisitorsState.Error }
    }

    private fun CoroutineScope.fetchRevenueDataAsync(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ) = async {
        analyticsRepository.fetchRevenueData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.RevenueResult.RevenueData }
            ?.let { _revenueState.value = RevenueState.Available(it.revenueStat) }
            ?: _revenueState.update { RevenueState.Error }
    }

    private fun CoroutineScope.fetchProductsDataAsync(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ) = async {
        analyticsRepository.fetchProductsData(rangeSelection, fetchStrategy)
            .run { this as? AnalyticsRepository.ProductsResult.ProductsData }
            ?.let { _productsState.value = ProductsState.Available(it.productsStat) }
            ?: _productsState.update { ProductsState.Error }
    }

    @Suppress("MagicNumber")
    private fun CoroutineScope.fetchBundlesDataAsync(
        rangeSelection: StatsTimeRangeSelection,
        fetchStrategy: FetchStrategy
    ) = async {
        delay(500)
        if (rangeSelection.currentRange.toString() == fetchStrategy.toString()) return@async
        _bundlesState.value = BundlesState.Available(
            BundleStat(
                bundlesSold = 200,
                bundlesSoldDelta = DeltaPercentage.Value(50),
                bundles = listOf(
                    BundleItem(
                        netSales = 3000.00,
                        name = "This is a sample bundle",
                        image = null,
                        quantity = 50,
                        currencyCode = null
                    ),
                    BundleItem(
                        netSales = 5000.00,
                        name = "This is another bundle",
                        image = null,
                        quantity = 50,
                        currencyCode = null
                    ),
                    BundleItem(
                        netSales = 5000.00,
                        name = "This is an amazing bundle",
                        image = null,
                        quantity = 50,
                        currencyCode = null
                    )
                )
            )
        )
    }
}
