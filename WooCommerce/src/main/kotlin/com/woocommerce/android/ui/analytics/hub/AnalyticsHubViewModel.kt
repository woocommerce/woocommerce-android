package com.woocommerce.android.ui.analytics.hub

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.RefreshIndicator.NotShowIndicator
import com.woocommerce.android.ui.analytics.hub.RefreshIndicator.ShowIndicator
import com.woocommerce.android.ui.analytics.hub.daterangeselector.AnalyticsHubDateRangeSelectorViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationSectionViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationViewState.DataViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationViewState.LoadingViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationViewState.NoDataState
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListCardItemViewState
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsHubUpdateState.Finished
import com.woocommerce.android.ui.analytics.hub.sync.OrdersState
import com.woocommerce.android.ui.analytics.hub.sync.ProductsState
import com.woocommerce.android.ui.analytics.hub.sync.RevenueState
import com.woocommerce.android.ui.analytics.hub.sync.SessionState
import com.woocommerce.android.ui.analytics.hub.sync.UpdateAnalyticsHubStats
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.feedback.FeedbackRepository
import com.woocommerce.android.ui.mystore.MyStoreStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.mystore.domain.ObserveLastUpdate
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListViewState as ProductsViewState
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListViewState.LoadingViewState as LoadingProductsViewState
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListViewState.NoDataState as ProductsNoDataState

@HiltViewModel
class AnalyticsHubViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val transactionLauncher: AnalyticsHubTransactionLauncher,
    private val usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter,
    private val updateStats: UpdateAnalyticsHubStats,
    private val observeLastUpdate: ObserveLastUpdate,
    private val localeProvider: LocaleProvider,
    private val feedbackRepository: FeedbackRepository,
    private val tracker: AnalyticsTrackerWrapper,
    private val dateUtils: DateUtils,
    private val selectedSite: SelectedSite,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: AnalyticsHubFragmentArgs by savedState.navArgs()

    val performanceObserver: LifecycleObserver = transactionLauncher

    private val rangeSelectionState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.targetGranularity.generateLocalizedSelectionData(selectedSite.getOrNull())
    )

    private val mutableState = MutableStateFlow(
        AnalyticsViewState(
            refreshIndicator = NotShowIndicator,
            analyticsDateRangeSelectorState = AnalyticsHubDateRangeSelectorViewState.EMPTY,
            revenueState = LoadingViewState,
            ordersState = LoadingViewState,
            productsState = LoadingProductsViewState,
            sessionState = LoadingViewState,
            showFeedBackBanner = false,
            lastUpdateTimestamp = ""
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

    private var lastUpdateObservationJob: Job? = null

    init {
        observeOrdersStatChanges()
        observeSessionChanges()
        observeProductsChanges()
        observeRevenueChanges()
        observeRangeSelectionChanges()
        observeLastUpdateTimestamp()
        shouldAskForFeedback()
    }

    private fun shouldAskForFeedback() {
        viewModelScope.launch {
            val feedbackStatus = feedbackRepository.getFeatureFeedbackState(
                FeatureFeedbackSettings.Feature.ANALYTICS_HUB
            )
            mutableState.update { viewState ->
                viewState.copy(showFeedBackBanner = feedbackStatus == FeatureFeedbackSettings.FeedbackState.UNANSWERED)
            }
        }
    }

    fun onNewRangeSelection(selectionType: SelectionType) {
        rangeSelectionState.value = selectionType.generateLocalizedSelectionData(
            siteModel = selectedSite.getOrNull()
        )
    }

    fun onCustomRangeSelected(startDate: Date, endDate: Date) {
        rangeSelectionState.value = SelectionType.CUSTOM.generateLocalizedSelectionData(
            siteModel = selectedSite.getOrNull(),
            startDate = startDate,
            endDate = endDate
        )
    }

    fun onCustomDateRangeClicked() {
        val startTimestamp = ranges.currentRange.start.time
        val endTimestamp = ranges.currentRange.end.time
        triggerEvent(AnalyticsViewEvent.OpenDatePicker(startTimestamp, endTimestamp))
    }

    fun onRefreshRequested() {
        tracker.track(AnalyticsEvent.ANALYTICS_HUB_PULL_TO_REFRESH_TRIGGERED)
        viewModelScope.launch {
            updateStats(
                rangeSelection = ranges,
                scope = viewModelScope,
                forceUpdate = true
            ).collect {
                mutableState.update { viewState ->
                    viewState.copy(refreshIndicator = if (it is Finished) NotShowIndicator else ShowIndicator)
                }
            }
        }
    }

    fun onDateRangeSelectorClick() {
        onTrackableUIInteraction()
        tracker.track(AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_BUTTON_TAPPED)
        triggerEvent(AnalyticsViewEvent.OpenDateRangeSelector)
    }

    fun onTrackableUIInteraction() = usageTracksEventEmitter.interacted()

    private fun formatValue(value: String, currencyCode: String?) =
        currencyCode?.let { currencyFormatter.formatCurrency(value, it) } ?: value

    private fun observeRangeSelectionChanges() {
        rangeSelectionState.onEach {
            observeLastUpdateTimestamp()
            updateDateSelector()
            trackSelectedDateRange()
            updateStats(
                rangeSelection = it,
                scope = viewModelScope
            )
        }.launchIn(viewModelScope)
    }

    private fun observeOrdersStatChanges() {
        updateStats.ordersState.onEach { state ->
            when (state) {
                is OrdersState.Available -> mutableState.update { viewState ->
                    viewState.copy(ordersState = buildOrdersDataViewState(state.orders))
                }

                is OrdersState.Error -> mutableState.update { viewState ->
                    val message = resourceProvider.getString(R.string.analytics_orders_no_data)
                    viewState.copy(ordersState = NoDataState(message))
                }

                is OrdersState.Loading -> mutableState.update { viewState ->
                    viewState.copy(ordersState = LoadingViewState)
                }
            }
        }
            .drop(1)
            .filter { state -> state is OrdersState.Available }
            .onEach { transactionLauncher.onOrdersFetched() }
            .launchIn(viewModelScope)
    }

    private fun observeSessionChanges() {
        updateStats.sessionState.onEach { state ->
            when (state) {
                is SessionState.Available -> mutableState.update { viewState ->
                    viewState.copy(sessionState = buildSessionViewState(state.session))
                }

                is SessionState.Error -> mutableState.update { viewState ->
                    val message = resourceProvider.getString(R.string.analytics_session_no_data)
                    viewState.copy(sessionState = NoDataState(message))
                }

                is SessionState.Loading -> mutableState.update { viewState ->
                    viewState.copy(sessionState = LoadingViewState)
                }
            }
        }
            .drop(1)
            .filter { state -> state is SessionState.Available }
            .onEach { transactionLauncher.onSessionFetched() }
            .launchIn(viewModelScope)
    }

    private fun observeProductsChanges() {
        updateStats.productsState.onEach { state ->
            when (state) {
                is ProductsState.Available -> mutableState.update { viewState ->
                    viewState.copy(productsState = buildProductsDataState(state.products))
                }

                is ProductsState.Error -> mutableState.update { viewState ->
                    val message = resourceProvider.getString(R.string.analytics_products_no_data)
                    viewState.copy(productsState = ProductsNoDataState(message))
                }

                is ProductsState.Loading -> mutableState.update { viewState ->
                    viewState.copy(productsState = ProductsViewState.LoadingViewState)
                }
            }
        }
            .drop(1)
            .filter { state -> state is ProductsState.Available }
            .onEach { transactionLauncher.onProductsFetched() }
            .launchIn(viewModelScope)
    }

    private fun observeRevenueChanges() {
        updateStats.revenueState.onEach { state ->
            when (state) {
                is RevenueState.Available -> mutableState.update { viewState ->
                    viewState.copy(revenueState = buildRevenueDataViewState(state.revenue))
                }

                is RevenueState.Error -> mutableState.update { viewState ->
                    val message = resourceProvider.getString(R.string.analytics_revenue_no_data)
                    viewState.copy(revenueState = NoDataState(message))
                }

                is RevenueState.Loading -> mutableState.update { viewState ->
                    viewState.copy(revenueState = LoadingViewState)
                }
            }
        }
            .drop(1)
            .filter { state -> state is RevenueState.Available }
            .onEach { transactionLauncher.onRevenueFetched() }
            .launchIn(viewModelScope)
    }

    private fun observeLastUpdateTimestamp() {
        lastUpdateObservationJob?.cancel()
        mutableState.value = viewState.value.copy(lastUpdateTimestamp = "")
        lastUpdateObservationJob = observeLastUpdate(timeRangeSelection = rangeSelectionState.value)
            .filterNotNull()
            .map { dateUtils.getDateOrTimeFromMillis(it) }
            .onEach { mutableState.value = viewState.value.copy(lastUpdateTimestamp = it.orEmpty()) }
            .launchIn(viewModelScope)
    }

    private fun updateDateSelector() {
        mutableState.value = viewState.value.copy(
            analyticsDateRangeSelectorState = viewState.value.analyticsDateRangeSelectorState.copy(
                previousRange = ranges.previousRangeDescription,
                currentRange = ranges.currentRangeDescription,
                selectionType = ranges.selectionType
            )
        )
    }

    private fun buildSessionViewState(
        stats: SessionStat
    ) = DataViewState(
        title = resourceProvider.getString(R.string.analytics_session_card_title),
        leftSection = AnalyticsHubInformationSectionViewState(
            resourceProvider.getString(R.string.analytics_visitors_subtitle),
            stats.visitorsCount.toString(),
            null,
            listOf()
        ),
        rightSection = AnalyticsHubInformationSectionViewState(
            resourceProvider.getString(R.string.analytics_conversion_subtitle),
            stats.conversionRate,
            null,
            listOf()
        )
    )

    private fun buildRevenueDataViewState(revenueStat: RevenueStat) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_revenue_card_title),
            leftSection = AnalyticsHubInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_sales_title),
                formatValue(revenueStat.totalValue.toString(), revenueStat.currencyCode),
                if (revenueStat.totalDelta is DeltaPercentage.Value) revenueStat.totalDelta.value else null,
                revenueStat.totalRevenueByInterval.map { it.toFloat() }
            ),
            rightSection = AnalyticsHubInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_net_sales_title),
                formatValue(revenueStat.netValue.toString(), revenueStat.currencyCode),
                if (revenueStat.netDelta is DeltaPercentage.Value) revenueStat.netDelta.value else null,
                revenueStat.netRevenueByInterval.map { it.toFloat() }
            ),
        )

    private fun buildOrdersDataViewState(ordersStats: OrdersStat) =
        DataViewState(
            title = resourceProvider.getString(R.string.analytics_orders_card_title),
            leftSection = AnalyticsHubInformationSectionViewState(
                resourceProvider.getString(R.string.analytics_total_orders_title),
                ordersStats.ordersCount.toString(),
                if (ordersStats.ordersCountDelta is DeltaPercentage.Value) {
                    ordersStats.ordersCountDelta.value
                } else {
                    null
                },
                ordersStats.ordersCountByInterval.map { it.toFloat() }
            ),
            rightSection = AnalyticsHubInformationSectionViewState(
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
                    AnalyticsHubListCardItemViewState(
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
        tracker.track(
            AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_SELECTED,
            mapOf(AnalyticsTracker.KEY_OPTION to ranges.selectionType.identifier)
        )
    }

    private fun SelectionType.generateLocalizedSelectionData(
        siteModel: SiteModel?,
        startDate: Date = Date(),
        endDate: Date = Date()
    ) = generateSelectionData(
        siteModel = siteModel,
        referenceStartDate = startDate,
        referenceEndDate = endDate,
        calendar = Calendar.getInstance(),
        locale = localeProvider.provideLocale() ?: Locale.getDefault()
    )

    fun onSendFeedbackClicked() {
        tracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_ANALYTICS_HUB_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        feedbackRepository.saveFeatureFeedback(
            FeatureFeedbackSettings.Feature.ANALYTICS_HUB,
            FeatureFeedbackSettings.FeedbackState.GIVEN
        )
        triggerEvent(AnalyticsViewEvent.SendFeedback)
        shouldAskForFeedback()
    }

    fun onDismissBannerClicked() {
        tracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_ANALYTICS_HUB_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        feedbackRepository.saveFeatureFeedback(
            FeatureFeedbackSettings.Feature.ANALYTICS_HUB,
            FeatureFeedbackSettings.FeedbackState.DISMISSED
        )
        shouldAskForFeedback()
    }
}
