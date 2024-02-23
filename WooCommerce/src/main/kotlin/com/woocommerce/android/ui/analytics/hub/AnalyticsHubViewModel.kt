package com.woocommerce.android.ui.analytics.hub

import android.util.Log
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
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
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsCardViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationSectionViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationViewState.DataViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationViewState.HiddenState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationViewState.LoadingViewState
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationViewState.NoDataState
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListCardItemViewState
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsHubUpdateState.Finished
import com.woocommerce.android.ui.analytics.hub.sync.OrdersState
import com.woocommerce.android.ui.analytics.hub.sync.ProductsState
import com.woocommerce.android.ui.analytics.hub.sync.RevenueState
import com.woocommerce.android.ui.analytics.hub.sync.SessionState
import com.woocommerce.android.ui.analytics.hub.sync.UpdateAnalyticsHubStats
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListViewState as ProductsViewState
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListViewState.HiddenState as ProductsHiddenDataState
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
    private val getReportUrl: GetReportUrl,
    private val observeAnalyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: AnalyticsHubFragmentArgs by savedState.navArgs()

    val performanceObserver: LifecycleObserver = transactionLauncher

    private val rangeSelectionState: MutableStateFlow<StatsTimeRangeSelection> = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.targetGranularity.generateLocalizedSelectionData()
    )

    private val mutableState = MutableStateFlow(
        AnalyticsViewState(
            refreshIndicator = NotShowIndicator,
            analyticsDateRangeSelectorState = AnalyticsHubDateRangeSelectorViewState.EMPTY,
            cards = AnalyticsHubCardViewState.LoadingCardsConfiguration,
            showFeedBackBanner = false,
            lastUpdateTimestamp = ""
        )
    )
    val viewState: StateFlow<AnalyticsViewState> = mutableState

    val selectableRangeOptions by lazy {
        SelectionType.entries
            .map { resourceProvider.getString(it.localizedResourceId) }
            .toTypedArray()
    }

    private val currentConfiguration: MutableStateFlow<List<AnalyticCardConfiguration>?> = MutableStateFlow(null)

    private val ranges
        get() = rangeSelectionState.value

    private var lastUpdateObservationJob: Job? = null
    private var revenueObservationJob: Job? = null
    private var sessionObservationJob: Job? = null
    private var productObservationJob: Job? = null
    private var ordersObservationJob: Job? = null

    init {
        observeConfigurationChanges()
        observeRangeSelectionChanges()
        observeLastUpdateTimestamp()
        shouldAskForFeedback()
        combineSelectionAndConfiguration()
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
        rangeSelectionState.value = selectionType.generateLocalizedSelectionData()
    }

    private fun observeConfigurationChanges() {
        launch {
            observeAnalyticsCardsConfiguration().collect { configuration ->
                val cardsState = mutableMapOf<AnalyticsCards, AnalyticsCardViewState>()
                cancelCardsObservation()
                configuration.forEach { cardConfiguration ->
                    when (cardConfiguration.card) {
                        AnalyticsCards.Revenue -> {
                            if (cardConfiguration.isVisible) {
                                cardsState[AnalyticsCards.Revenue] = LoadingViewState
                                observeRevenueChanges()
                            } else {
                                cardsState[AnalyticsCards.Revenue] = HiddenState
                            }
                        }

                        AnalyticsCards.Orders -> {
                            if (cardConfiguration.isVisible) {
                                cardsState[AnalyticsCards.Orders] = LoadingViewState
                                observeOrdersStatChanges()
                            } else {
                                cardsState[AnalyticsCards.Orders] = HiddenState
                            }
                        }

                        AnalyticsCards.Products -> {
                            if (cardConfiguration.isVisible) {
                                cardsState[AnalyticsCards.Products] = LoadingProductsViewState
                                observeProductsChanges()
                            } else {
                                cardsState[AnalyticsCards.Products] = ProductsHiddenDataState
                            }
                        }

                        AnalyticsCards.Session -> {
                            if (cardConfiguration.isVisible) {
                                cardsState[AnalyticsCards.Session] = LoadingViewState
                                observeSessionChanges()
                            } else {
                                cardsState[AnalyticsCards.Session] = HiddenState
                            }
                        }
                    }
                }
                mutableState.update { viewState ->
                    viewState.copy(cards = AnalyticsHubCardViewState.CardsState(cardsState))
                }

                currentConfiguration.value = configuration
            }
        }
    }

    fun onSeeReport(url: String, card: ReportCard) {
        trackSeeReportInteraction(card)
        selectedSite.getOrNull()?.let { site ->
            val event = if (site.isWpComStore) {
                AnalyticsViewEvent.OpenWPComWebView(url)
            } else {
                AnalyticsViewEvent.OpenUrl(url)
            }
            triggerEvent(event)
        } ?: triggerEvent(AnalyticsViewEvent.OpenUrl(url))
    }

    private fun trackSeeReportInteraction(card: ReportCard) {
        onTrackableUIInteraction()
        val period = ranges.selectionType.identifier
        val report = card.name.lowercase()
        tracker.track(
            AnalyticsEvent.ANALYTICS_HUB_VIEW_FULL_REPORT_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_PERIOD to period,
                AnalyticsTracker.KEY_REPORT to report,
                AnalyticsTracker.KEY_COMPARE to AnalyticsTracker.VALUE_PREVIOUS_PERIOD
            )
        )
    }

    fun onCustomRangeSelected(startDate: Date, endDate: Date) {
        rangeSelectionState.value = SelectionType.CUSTOM.generateLocalizedSelectionData(
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
        val visibleCards = currentConfiguration.value?.filter { it.isVisible }?.map { it.card }?.toSet()
            ?: AnalyticsCards.entries.toSet()
        viewModelScope.launch {
            updateStats(
                rangeSelection = ranges,
                scope = viewModelScope,
                forceUpdate = true,
                visibleCards = visibleCards
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
        }.launchIn(viewModelScope)
    }

    private fun combineSelectionAndConfiguration() {
        combine(currentConfiguration.filterNotNull(), rangeSelectionState) { configuration, selection ->
            Log.d("HUB", "data updated")
            updateStats(
                rangeSelection = selection,
                scope = viewModelScope,
                visibleCards = configuration.filter { it.isVisible }.map { it.card }.toSet()
            )
        }.launchIn(viewModelScope)
    }

    private fun observeOrdersStatChanges() {
        ordersObservationJob = updateStats.ordersState.onEach { state ->
            when (state) {
                is OrdersState.Available -> {
                    updateCardStatus(AnalyticsCards.Orders, buildOrdersDataViewState(state.orders))
                }

                is OrdersState.Error -> {
                    val message = resourceProvider.getString(R.string.analytics_orders_no_data)
                    updateCardStatus(AnalyticsCards.Orders, NoDataState(message))
                }

                is OrdersState.Loading -> {
                    updateCardStatus(AnalyticsCards.Orders, LoadingViewState)
                }
            }
        }
            .drop(1)
            .filter { state -> state is OrdersState.Available }
            .onEach { transactionLauncher.onOrdersFetched() }
            .launchIn(viewModelScope)
    }

    private fun observeSessionChanges() {
        sessionObservationJob = updateStats.sessionState.onEach { state ->
            when (state) {
                is SessionState.Available -> {
                    updateCardStatus(AnalyticsCards.Session, buildSessionViewState(state.session))
                }

                is SessionState.Error -> {
                    val message = resourceProvider.getString(R.string.analytics_session_no_data)
                    updateCardStatus(AnalyticsCards.Session, NoDataState(message))
                }

                is SessionState.Loading -> {
                    updateCardStatus(AnalyticsCards.Session, LoadingViewState)
                }
            }
        }
            .drop(1)
            .filter { state -> state is SessionState.Available }
            .onEach { transactionLauncher.onSessionFetched() }
            .launchIn(viewModelScope)
    }

    private fun observeProductsChanges() {
        productObservationJob = updateStats.productsState.onEach { state ->
            when (state) {
                is ProductsState.Available -> {
                    updateCardStatus(AnalyticsCards.Products, buildProductsDataState(state.products))
                }

                is ProductsState.Error -> {
                    val message = resourceProvider.getString(R.string.analytics_products_no_data)
                    updateCardStatus(AnalyticsCards.Products, ProductsNoDataState(message))
                }

                is ProductsState.Loading -> {
                    updateCardStatus(AnalyticsCards.Products, LoadingProductsViewState)
                }
            }
        }
            .drop(1)
            .filter { state -> state is ProductsState.Available }
            .onEach { transactionLauncher.onProductsFetched() }
            .launchIn(viewModelScope)
    }

    private fun observeRevenueChanges() {
        revenueObservationJob = updateStats.revenueState.onEach { state ->
            when (state) {
                is RevenueState.Available -> {
                    updateCardStatus(AnalyticsCards.Revenue, buildRevenueDataViewState(state.revenue))
                }

                is RevenueState.Error -> {
                    val message = resourceProvider.getString(R.string.analytics_revenue_no_data)
                    updateCardStatus(AnalyticsCards.Revenue, NoDataState(message))
                }

                is RevenueState.Loading -> {
                    updateCardStatus(AnalyticsCards.Revenue, LoadingViewState)
                }
            }
        }
            .drop(1)
            .filter { state -> state is RevenueState.Available }
            .onEach { transactionLauncher.onRevenueFetched() }
            .launchIn(viewModelScope)
    }

    private fun updateCardStatus(card: AnalyticsCards, state: AnalyticsCardViewState) {
        val cardsInformation = mutableState.value.cards
        if (
            cardsInformation is AnalyticsHubCardViewState.CardsState &&
            cardsInformation.cardsState.containsKey(card)
        ) {
            val updatedCardState = cardsInformation.cardsState.toMutableMap()
            updatedCardState[card] = state
            val updatedInfo = cardsInformation.copy(cardsState = updatedCardState)
            mutableState.update { viewState ->
                viewState.copy(cards = updatedInfo)
            }
        }
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
        ),
        reportUrl = null
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
            reportUrl = getReportUrl(
                selection = ranges,
                card = ReportCard.Revenue
            )
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
            ),
            reportUrl = getReportUrl(
                selection = ranges,
                card = ReportCard.Orders
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
                },
            reportUrl = getReportUrl(
                selection = ranges,
                card = ReportCard.Products
            )
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
        startDate: Date = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
        endDate: Date = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
    ) = generateSelectionData(
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

    private fun cancelCardsObservation() {
        revenueObservationJob?.cancel()
        ordersObservationJob?.cancel()
        productObservationJob?.cancel()
        sessionObservationJob?.cancel()
    }
}

enum class ReportCard { Revenue, Orders, Products }
