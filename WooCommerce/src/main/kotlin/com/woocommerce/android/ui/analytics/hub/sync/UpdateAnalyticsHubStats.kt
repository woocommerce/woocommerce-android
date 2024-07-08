package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.model.BundleStat
import com.woocommerce.android.model.GiftCardsStat
import com.woocommerce.android.model.GoogleAdsStat
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

    private val _giftCardsState = MutableStateFlow(GiftCardsState.Available(GiftCardsStat.EMPTY) as GiftCardsState)
    val giftCardsState: Flow<GiftCardsState> = _giftCardsState

    private val _googleAdsState = MutableStateFlow(GoogleAdsState.Available(GoogleAdsStat.EMPTY) as GoogleAdsState)
    val googleAdsState: Flow<GoogleAdsState> = _googleAdsState

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
                AnalyticsCards.GiftCards -> _giftCardsState.update { GiftCardsState.Loading }
                AnalyticsCards.GoogleAds -> _googleAdsState.update { GoogleAdsState.Loading }
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
                AnalyticsCards.Bundles -> scope.fetchBundlesDataAsync(rangeSelection)
                AnalyticsCards.GiftCards -> scope.fetchGiftCardDataAsync(rangeSelection)
                AnalyticsCards.GoogleAds -> scope.fetchGoogleAdsAsync(rangeSelection)
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

                AnalyticsCards.GiftCards -> analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection,
                    AnalyticsUpdateDataStore.AnalyticData.GIFT_CARDS
                )

                AnalyticsCards.GoogleAds -> analyticsUpdateDataStore.storeLastAnalyticsUpdate(
                    rangeSelection,
                    AnalyticsUpdateDataStore.AnalyticData.GOOGLE_ADS
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

    @Suppress("MagicNumber")
    private fun combineFullUpdateState() =
        combine(
            _revenueState,
            _productsState,
            _ordersState,
            sessionState,
            _bundlesState,
            _giftCardsState
        ) { flows ->
            val revenue = flows[0] as RevenueState
            val products = flows[1] as ProductsState
            val orders = flows[2] as OrdersState
            val session = flows[3] as SessionState
            val bundles = flows[4] as BundlesState
            val giftCard = flows[5] as GiftCardsState
            revenue.isIdle && products.isIdle && orders.isIdle && session.isIdle && bundles.isIdle && giftCard.isIdle
        }.map { if (it) Finished else Loading }

    private fun combineSessionDataChanges() =
        combine(_ordersState, visitorsCountState) { orders, visitors ->
            when {
                orders is OrdersState.Available && visitors is VisitorsState.Available -> {
                    SessionState.Available(SessionStat(orders.orders.ordersCount, visitors.visitors))
                }
                visitors is VisitorsState.NotSupported -> {
                    SessionState.NotSupported
                }
                orders is OrdersState.Error || visitors is VisitorsState.Error -> {
                    SessionState.Error
                }
                else -> {
                    SessionState.Loading
                }
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
        analyticsRepository.fetchVisitorsData(rangeSelection, fetchStrategy).run {
            when (this) {
                is AnalyticsRepository.VisitorsResult.VisitorsData -> {
                    visitorsCountState.value = VisitorsState.Available(visitorsCount)
                }
                is AnalyticsRepository.VisitorsResult.VisitorsError -> {
                    visitorsCountState.update { VisitorsState.Error }
                }
                is AnalyticsRepository.VisitorsResult.VisitorsNotSupported -> {
                    visitorsCountState.update { VisitorsState.NotSupported }
                }
            }
        }
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

    private fun CoroutineScope.fetchBundlesDataAsync(
        rangeSelection: StatsTimeRangeSelection
    ) = async {
        analyticsRepository.fetchProductBundlesStats(rangeSelection)
            .run { this as? AnalyticsRepository.BundlesResult.BundlesData }
            ?.let { _bundlesState.value = BundlesState.Available(it.bundleStat) }
            ?: _bundlesState.update { BundlesState.Error }
    }

    private fun CoroutineScope.fetchGiftCardDataAsync(
        rangeSelection: StatsTimeRangeSelection
    ) = async {
        analyticsRepository.fetchGiftCardsStats(rangeSelection)
            .run { this as? AnalyticsRepository.GiftCardResult.GiftCardData }
            ?.let { _giftCardsState.value = GiftCardsState.Available(it.giftCardStat) }
            ?: _giftCardsState.update { GiftCardsState.Error }
    }

    private fun CoroutineScope.fetchGoogleAdsAsync(
        rangeSelection: StatsTimeRangeSelection
    ) = async {
        analyticsRepository.fetchGoogleAdsStats(rangeSelection)
            .run { this as? AnalyticsRepository.GoogleAdsResult.GoogleAdsData }
            ?.let { _googleAdsState.value = GoogleAdsState.Available(it.googleAdsStat) }
            ?: _googleAdsState.update { GoogleAdsState.Error }
    }
}
