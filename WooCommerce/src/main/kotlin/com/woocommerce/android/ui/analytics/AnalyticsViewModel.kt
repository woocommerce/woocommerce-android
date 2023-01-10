package com.woocommerce.android.ui.analytics

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.AnalyticsHubUpdateState.Finished
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.ForceNew
import com.woocommerce.android.ui.analytics.AnalyticsRepository.FetchStrategy.Saved
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState as ProductsViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState.LoadingViewState as LoadingProductsViewState
import com.woocommerce.android.ui.analytics.listcard.AnalyticsListViewState.NoDataState as ProductsNoDataState

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val transactionLauncher: AnalyticsHubTransactionLauncher,
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter,
    private val updateStats: UpdateAnalyticsHubStats,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: AnalyticsFragmentArgs by savedState.navArgs()

    val performanceObserver: LifecycleObserver = transactionLauncher

    private val rangeSelectionState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.targetGranularity.generateSelectionData()
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
        observeOrdersStatChanges()
        observeSessionChanges()
        observeProductsChanges()
        observeRevenueChanges()
        observeRangeSelectionChanges()
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
            updateStats(
                rangeSelection = ranges,
                fetchStrategy = getFetchStrategy(isRefreshing = true)
            ).collect {
                mutableState.update { viewState ->
                    viewState.copy(
                        refreshIndicator = if (it is Finished) NotShowIndicator else ShowIndicator
                    )
                }
            }
        }
    }

    fun onDateRangeSelectorClick() {
        onTrackableUIInteraction()
        AnalyticsTracker.track(AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_BUTTON_TAPPED)
        triggerEvent(AnalyticsViewEvent.OpenDateRangeSelector)
    }

    fun onTrackableUIInteraction() = usageTracksEventEmitter.interacted()

    private fun formatValue(value: String, currencyCode: String?) = currencyCode
        ?.let { currencyFormatter.formatCurrency(value, it) }
        ?: value

    private fun getFetchStrategy(isRefreshing: Boolean) = if (isRefreshing) ForceNew else Saved

    private fun observeRangeSelectionChanges() = viewModelScope.launch {
        rangeSelectionState.collect {
            updateDateSelector()
            trackSelectedDateRange()
            updateStats(
                rangeSelection = it,
                fetchStrategy = getFetchStrategy(isRefreshing = false)
            )
        }
    }

    private fun observeOrdersStatChanges() = viewModelScope.launch {
        updateStats.ordersState.collect { state ->
            when (state) {
                is OrdersState.Available -> mutableState.update { viewState ->
                    transactionLauncher.onOrdersFetched()
                    viewState.copy(ordersState = buildOrdersDataViewState(state.orders))
                }
                is OrdersState.Error -> mutableState.update { viewState ->
                    NoDataState(resourceProvider.getString(R.string.analytics_orders_no_data))
                        .let { viewState.copy(ordersState = it) }
                }
                is OrdersState.Loading -> mutableState.update { viewState ->
                    LoadingViewState.let { viewState.copy(ordersState = it) }
                }
            }
        }
    }

    private fun observeSessionChanges() = viewModelScope.launch {
        updateStats.sessionState.collect { state ->
            when (state) {
                is SessionState.Available -> mutableState.update { viewState ->
                    transactionLauncher.onSessionFetched()
                    viewState.copy(
                        sessionState = buildSessionViewState(state.session)
                    )
                }
                is SessionState.Error -> mutableState.update { viewState ->
                    viewState.copy(
                        sessionState = NoDataState("No session data")
                    )
                }
                is SessionState.Loading -> mutableState.update { viewState ->
                    LoadingViewState.let { viewState.copy(sessionState = it) }
                }
            }
        }
    }

    private fun observeProductsChanges() = viewModelScope.launch {
        updateStats.productsState.collect { state ->
            when (state) {
                is ProductsState.Available -> mutableState.update { viewState ->
                    transactionLauncher.onProductsFetched()
                    viewState.copy(
                        productsState = buildProductsDataState(state.products)
                    )
                }
                is ProductsState.Error -> mutableState.update { viewState ->
                    viewState.copy(
                        productsState = ProductsNoDataState(
                            resourceProvider.getString(R.string.analytics_products_no_data)
                        )
                    )
                }
                is ProductsState.Loading -> mutableState.update { viewState ->
                    ProductsViewState.LoadingViewState.let { viewState.copy(productsState = it) }
                }
            }
        }
    }

    private fun observeRevenueChanges() = viewModelScope.launch {
        updateStats.revenueState.collect { state ->
            when (state) {
                is RevenueState.Available -> mutableState.update { viewState ->
                    transactionLauncher.onRevenueFetched()
                    viewState.copy(
                        revenueState = buildRevenueDataViewState(state.revenue)
                    )
                }
                is RevenueState.Error -> mutableState.update { viewState ->
                    viewState.copy(
                        revenueState = NoDataState(resourceProvider.getString(R.string.analytics_revenue_no_data))
                    )
                }
                is RevenueState.Loading -> mutableState.update { viewState ->
                    LoadingViewState.let { viewState.copy(revenueState = it) }
                }
            }
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

    private fun buildSessionViewState(
        stats: SessionStat
    ) = DataViewState(
        title = resourceProvider.getString(R.string.analytics_session_card_title),
        leftSection = AnalyticsInformationSectionViewState(
            resourceProvider.getString(R.string.analytics_views_subtitle),
            stats.visitorsCount.toString(),
            null,
            listOf()
        ),
        rightSection = AnalyticsInformationSectionViewState(
            resourceProvider.getString(R.string.analytics_conversion_subtitle),
            stats.conversionRate,
            null,
            listOf()
        )
    )

    private fun buildRevenueDataViewState(revenueStat: RevenueStat) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_revenue_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_sales_title),
                formatValue(revenueStat.totalValue.toString(), revenueStat.currencyCode),
                if (revenueStat.totalDelta is DeltaPercentage.Value) revenueStat.totalDelta.value else null,
                revenueStat.totalRevenueByInterval.map { it.toFloat() }
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_net_sales_title),
                formatValue(revenueStat.netValue.toString(), revenueStat.currencyCode),
                if (revenueStat.netDelta is DeltaPercentage.Value) revenueStat.netDelta.value else null,
                revenueStat.netRevenueByInterval.map { it.toFloat() }
            ),
        )

    private fun buildOrdersDataViewState(ordersStats: OrdersStat) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_orders_card_title),
            leftSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_orders_title),
                ordersStats.ordersCount.toString(),
                if (ordersStats.ordersCountDelta is DeltaPercentage.Value) {
                    ordersStats.ordersCountDelta.value
                } else {
                    null
                },
                ordersStats.ordersCountByInterval.map { it.toFloat() }
            ),
            rightSection = AnalyticsInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_avg_orders_title),
                formatValue(ordersStats.avgOrderValue.toString(), ordersStats.currencyCode),
                if (ordersStats.avgOrderDelta is DeltaPercentage.Value) {
                    ordersStats.avgOrderDelta.value
                } else {
                    null
                },
                ordersStats.avgOrderValueByInterval.map { it.toFloat() }
            )
        )

    private fun buildProductsDataState(productsStat: ProductsStat): ProductsViewState.DataViewState {
        val itemsSold = productsStat.itemsSold
        val delta = productsStat.itemsSoldDelta
        val products = productsStat.products
        return ProductsViewState.DataViewState(
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
    }

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
