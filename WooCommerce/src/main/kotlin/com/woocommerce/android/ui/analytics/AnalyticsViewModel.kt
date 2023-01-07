package com.woocommerce.android.ui.analytics

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.ProductItem
import com.woocommerce.android.model.SessionStats
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.Saved
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult.OrdersError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.ProductsResult.ProductsError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.RevenueResult.RevenueError
import com.woocommerce.android.ui.analytics.AnalyticsRepository.SessionResult
import com.woocommerce.android.ui.analytics.AnalyticsRepository.SessionResult.SessionData
import com.woocommerce.android.ui.analytics.AnalyticsRepository.SessionResult.SessionError
import com.woocommerce.android.ui.analytics.RefreshIndicator.NotShowIndicator
import com.woocommerce.android.ui.analytics.RefreshIndicator.ShowIndicator
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationSectionViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.DataViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.LoadingViewState
import com.woocommerce.android.ui.analytics.informationcard.AnalyticsInformationViewState.NoDataState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListCardItemViewState
import com.woocommerce.android.ui.analytics.ranges.AnalyticsHubDateRangeSelection.SelectionType
import com.woocommerce.android.ui.mystore.MyStoreStatsUsageTracksEventEmitter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState as ProductsViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState.LoadingViewState as LoadingProductsViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState.NoDataState as ProductsNoDataState
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.ui.analytics.AnalyticsRepository.OrdersResult
import kotlinx.coroutines.flow.update

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val analyticsRepository: AnalyticsRepository,
    private val transactionLauncher: AnalyticsHubTransactionLauncher,
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: AnalyticsFragmentArgs by savedState.navArgs()

    val performanceObserver: LifecycleObserver = transactionLauncher

    private val rangeSelectionState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.targetGranularity.generateSelectionData()
    )

    private val ordersDataState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = OrdersData(OrdersStat.EMPTY) as OrdersResult
    )

    private val mutableState = MutableStateFlow(
        AnalyticsViewState(
            NotShowIndicator,
            AnalyticsDateRangeSelectorViewState.EMPTY,
            LoadingViewState,
            LoadingViewState,
            LoadingProductsViewState,
            LoadingViewState,
        )
    )
    val viewState: StateFlow<AnalyticsViewState> = mutableState

    val selectableRangeOptions by lazy {
        SelectionType.values()
            .map { resourceProvider.getString(it.localizedResourceId) }
            .toTypedArray()
    }

    private val ranges
        get() = rangeSelectionState.value

    init {
        viewModelScope.launch {
            rangeSelectionState.collect {
                updateDateSelector()
                trackSelectedDateRange()
                refreshAllAnalyticsAtOnce(isRefreshing = false, showSkeleton = true)
            }
        }
        observeOrdersResult()
    }

    private fun observeOrdersResult() {
        viewModelScope.launch {
            ordersDataState.collect { orders ->
                when (orders) {
                    is OrdersData -> mutableState.update { viewState ->
                        transactionLauncher.onOrdersFetched()
                        viewState.copy(ordersState = buildOrdersDataViewState(orders))
                    }
                    is OrdersError -> mutableState.update { viewState ->
                        NoDataState(resourceProvider.getString(R.string.analytics_orders_no_data))
                            .let { viewState.copy(ordersState = it) }
                    }
                }

            }
        }
    }

    fun onNewRangeSelection(selectionType: SelectionType) {
        rangeSelectionState.value = selectionType.generateSelectionData()
    }

    fun onCustomRangeSelected(startDate: Date, endDate: Date) {
        rangeSelectionState.value = SelectionType.CUSTOM.generateSelectionData(startDate, endDate)
    }

    fun onCustomDateRangeClicked() {
        val fromMillis = ranges.currentRange.start.time
        val toMillis = ranges.currentRange.end.time
        triggerEvent(AnalyticsViewEvent.OpenDatePicker(fromMillis, toMillis))
    }

    fun onRefreshRequested() {
        viewModelScope.launch {
            refreshAllAnalyticsAtOnce(isRefreshing = true, showSkeleton = false)
        }
    }

    fun onDateRangeSelectorClick() {
        onTrackableUIInteraction()
        AnalyticsTracker.track(AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_BUTTON_TAPPED)
        triggerEvent(AnalyticsViewEvent.OpenDateRangeSelector)
    }

    fun onTrackableUIInteraction() = usageTracksEventEmitter.interacted()

    private fun refreshAllAnalyticsAtOnce(isRefreshing: Boolean, showSkeleton: Boolean) {
        updateRevenue(isRefreshing, showSkeleton)
        updateOrders(isRefreshing, showSkeleton)
        updateProducts(isRefreshing, showSkeleton)
        updateSessions(isRefreshing, showSkeleton)
    }

    private fun updateRevenue(isRefreshing: Boolean, showSkeleton: Boolean) =
        launch {
            val fetchStrategy = getFetchStrategy(isRefreshing)

            if (showSkeleton) mutableState.value = viewState.value.copy(revenueState = LoadingViewState)
            mutableState.value = viewState.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )

            analyticsRepository.fetchRevenueData(rangeSelectionState.value, fetchStrategy)
                .let {
                    when (it) {
                        is RevenueData -> {
                            mutableState.value = viewState.value.copy(
                                refreshIndicator = NotShowIndicator,
                                revenueState = buildRevenueDataViewState(it)
                            )
                            transactionLauncher.onRevenueFetched()
                        }
                        is RevenueError -> mutableState.value = viewState.value.copy(
                            refreshIndicator = NotShowIndicator,
                            revenueState = NoDataState(resourceProvider.getString(R.string.analytics_revenue_no_data))
                        )
                    }
                }
        }

    private fun updateOrders(isRefreshing: Boolean, showSkeleton: Boolean) =
        launch {
            val fetchStrategy = getFetchStrategy(isRefreshing)

            if (showSkeleton) mutableState.value = viewState.value.copy(ordersState = LoadingViewState)
            mutableState.value = viewState.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )
            analyticsRepository.fetchOrdersData(rangeSelectionState.value, fetchStrategy)
                .let { ordersDataState.value = it }
        }

    private fun updateProducts(isRefreshing: Boolean, showSkeleton: Boolean) =
        launch {
            val fetchStrategy = getFetchStrategy(isRefreshing)
            if (showSkeleton) mutableState.value = viewState.value.copy(productsState = LoadingProductsViewState)
            mutableState.value = viewState.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )
            analyticsRepository.fetchProductsData(rangeSelectionState.value, fetchStrategy)
                .let {
                    when (it) {
                        is ProductsData -> {
                            mutableState.value = viewState.value.copy(
                                productsState = buildProductsDataState(
                                    it.productsStat.itemsSold,
                                    it.productsStat.itemsSoldDelta,
                                    it.productsStat.products
                                )
                            )
                            transactionLauncher.onProductsFetched()
                        }
                        ProductsError -> mutableState.value = viewState.value.copy(
                            productsState = ProductsNoDataState(
                                resourceProvider.getString(R.string.analytics_products_no_data)
                            )
                        )
                    }
                }
        }

    private fun updateSessions(isRefreshing: Boolean, showSkeleton: Boolean) =
        launch {
            val fetchStrategy = getFetchStrategy(isRefreshing)

            if (showSkeleton) mutableState.value = viewState.value.copy(sessionState = LoadingViewState)
            mutableState.value = viewState.value.copy(
                refreshIndicator = if (isRefreshing) ShowIndicator else NotShowIndicator
            )

            analyticsRepository.fetchSessionData(rangeSelectionState.value, fetchStrategy)
                .handleSessionResult()
        }

    private fun SessionResult.handleSessionResult() {
        when (this) {
            is SessionData -> {
                mutableState.value = viewState.value.copy(
                    refreshIndicator = NotShowIndicator,
                    sessionState = buildSessionViewState(sessionStats)
                )
                transactionLauncher.onSessionFetched()
            }
            is SessionError -> mutableState.value = viewState.value.copy(
                refreshIndicator = NotShowIndicator,
                sessionState = NoDataState("No session data")
            )
        }
    }

    private fun updateDateSelector() {
        mutableState.value = viewState.value.copy(
            analyticsDateRangeSelectorState = viewState.value.analyticsDateRangeSelectorState.copy(
                previousRange = ranges.previousRangeDescription,
                currentRange = ranges.currentRangeDescription,
                selectionTitle = resourceProvider.getString(ranges.selectionType.localizedResourceId)
            )
        )
    }

    private fun formatValue(value: String, currencyCode: String?) = currencyCode
        ?.let { currencyFormatter.formatCurrency(value, it) }
        ?: value

    private fun buildSessionViewState(
        stats: SessionStats
    ) = DataViewState(
        title = resourceProvider.getString(R.string.analytics_session_card_title),
        leftSection = AnalyticsInformationSectionViewState(
            resourceProvider.getString(R.string.analytics_views_subtitle),
            stats.viewsCount.toString(),
            null,
            listOf()
        ),
        rightSection = AnalyticsInformationSectionViewState(
            resourceProvider.getString(R.string.analytics_conversion_subtitle),
            stats.conversionRate.toString(),
            null,
            listOf()
        )
    )

    private fun buildRevenueDataViewState(data: RevenueData) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_revenue_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_sales_title),
                formatValue(data.revenueStat.totalValue.toString(), data.revenueStat.currencyCode),
                if (data.revenueStat.totalDelta is DeltaPercentage.Value) data.revenueStat.totalDelta.value else null,
                data.revenueStat.totalRevenueByInterval.map { it.toFloat() }
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_net_sales_title),
                formatValue(data.revenueStat.netValue.toString(), data.revenueStat.currencyCode),
                if (data.revenueStat.netDelta is DeltaPercentage.Value) data.revenueStat.netDelta.value else null,
                data.revenueStat.netRevenueByInterval.map { it.toFloat() }
            ),
        )

    private fun buildOrdersDataViewState(data: OrdersData) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_orders_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_orders_title),
                data.ordersStat.ordersCount.toString(),
                if (data.ordersStat.ordersCountDelta is DeltaPercentage.Value) {
                    data.ordersStat.ordersCountDelta.value
                } else {
                    null
                },
                data.ordersStat.ordersCountByInterval.map { it.toFloat() }
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_avg_orders_title),
                formatValue(data.ordersStat.avgOrderValue.toString(), data.ordersStat.currencyCode),
                if (data.ordersStat.avgOrderDelta is DeltaPercentage.Value) {
                    data.ordersStat.avgOrderDelta.value
                } else {
                    null
                },
                data.ordersStat.avgOrderValueByInterval.map { it.toFloat() }
            )
        )

    private fun buildProductsDataState(itemsSold: Int, delta: DeltaPercentage, products: List<ProductItem>) =
        ProductsViewState.DataViewState(
            title = resourceProvider.getString(R.string.analytics_products_card_title),
            subTitle = resourceProvider.getString(R.string.analytics_products_list_items_sold),
            subTitleValue = itemsSold.toString(),
            delta = if (delta is DeltaPercentage.Value) delta.value else null,
            listLeftHeader = resourceProvider.getString(R.string.analytics_products_list_header_title),
            listRightHeader = resourceProvider.getString(R.string.analytics_products_list_header_subtitle),
            items = products
                .sortedByDescending { it.quantity }
                .mapIndexed { index, product ->
                    AnalyticsListCardItemViewState(
                        product.image,
                        product.name,
                        product.quantity.toString(),
                        resourceProvider.getString(
                            R.string.analytics_products_list_item_description,
                            formatValue(product.netSales.toString(), product.currencyCode)
                        ),
                        index != products.size - 1
                    )
                }
        )

    private fun getFetchStrategy(isRefreshing: Boolean) = if (isRefreshing) ForceNew else Saved

    private fun trackSelectedDateRange() {
        onTrackableUIInteraction()
        AnalyticsTracker.track(
            AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_SELECTED,
            mapOf(
                AnalyticsTracker.KEY_OPTION to ranges.selectionType.description
            )
        )
    }
}
