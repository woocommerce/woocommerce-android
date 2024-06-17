package com.woocommerce.android.ui.dashboard.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardDateRangeHeader
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WCAnalyticsNotAvailableErrorView
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import java.util.Date

@Composable
fun DashboardStatsCard(
    openDatePicker: (Long, Long, (Long, Long) -> Unit) -> Unit,
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardStatsViewModel = viewModelWithFactory(
        creationCallback = { factory: DashboardStatsViewModel.Factory ->
            factory.create(parentViewModel)
        }
    )
) {
    val dateRange by viewModel.dateRangeState.observeAsState()
    val revenueStatsState by viewModel.revenueStatsState.observeAsState()
    val visitorsStatsState by viewModel.visitorStatsState.observeAsState()
    val lastUpdateState by viewModel.lastUpdateStats.observeAsState()

    HandleEvents(
        event = viewModel.event,
        openDatePicker = { fromDate, toDate ->
            openDatePicker(fromDate, toDate) { from, to ->
                viewModel.onCustomRangeSelected(StatsTimeRange(Date(from), Date(to)))
            }
        }
    )

    WidgetCard(
        titleResource = DashboardWidget.Type.STATS.titleResource,
        menu = DashboardViewModel.DashboardWidgetMenu(
            items = listOf(
                DashboardWidget.Type.STATS.defaultHideMenuEntry {
                    parentViewModel.onHideWidgetClicked(DashboardWidget.Type.STATS)
                }
            )
        ),
        button = DashboardViewModel.DashboardWidgetAction(
            titleResource = R.string.analytics_section_see_all,
            action = viewModel::onViewAnalyticsClicked
        ),
        isError = revenueStatsState is DashboardStatsViewModel.RevenueStatsViewState.PluginNotActiveError ||
            revenueStatsState == DashboardStatsViewModel.RevenueStatsViewState.GenericError,
        modifier = modifier.testTag(DashboardStatsTestTags.DASHBOARD_STATS_CARD)
    ) {
        when (revenueStatsState) {
            is DashboardStatsViewModel.RevenueStatsViewState.GenericError -> {
                WidgetError(
                    onContactSupportClicked = parentViewModel::onContactSupportClicked,
                    onRetryClicked = viewModel::onRefresh
                )
            }

            !is DashboardStatsViewModel.RevenueStatsViewState.PluginNotActiveError -> {
                DashboardStatsContent(
                    dateRange = dateRange,
                    revenueStatsState = revenueStatsState,
                    visitorsStatsState = visitorsStatsState,
                    lastUpdateState = lastUpdateState,
                    dateUtils = viewModel.dateUtils,
                    currencyFormatter = viewModel.currencyFormatter,
                    usageTracksEventEmitter = viewModel.usageTracksEventEmitter,
                    onAddCustomRangeClick = viewModel::onAddCustomRangeClicked,
                    onTabSelected = viewModel::onTabSelected,
                    onChartDateSelected = viewModel::onChartDateSelected
                )
            }

            else -> {
                WCAnalyticsNotAvailableErrorView(
                    title = stringResource(id = R.string.my_store_stats_plugin_inactive_title),
                    onContactSupportClick = parentViewModel::onContactSupportClicked
                )
            }
        }
    }
}

@Composable
private fun DashboardStatsContent(
    dateRange: DashboardStatsViewModel.DateRangeState?,
    revenueStatsState: DashboardStatsViewModel.RevenueStatsViewState?,
    visitorsStatsState: DashboardStatsViewModel.VisitorStatsViewState?,
    lastUpdateState: Long?,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onAddCustomRangeClick: () -> Unit,
    onTabSelected: (SelectionType) -> Unit,
    onChartDateSelected: (String?) -> Unit,
) {
    Column {
        dateRange?.let {
            DashboardDateRangeHeader(
                rangeSelection = it.rangeSelection,
                dateFormatted = dateRange.selectedDateFormatted ?: dateRange.rangeFormatted,
                onCustomRangeClick = onAddCustomRangeClick,
                onTabSelected = onTabSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Divider()
        StatsChart(
            dateRange = dateRange,
            revenueStatsState = revenueStatsState,
            visitorsStatsState = visitorsStatsState,
            lastUpdateState = lastUpdateState,
            dateUtils = dateUtils,
            currencyFormatter = currencyFormatter,
            usageTracksEventEmitter = usageTracksEventEmitter,
            onAddCustomRangeClick = onAddCustomRangeClick,
            onChartDateSelected = onChartDateSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatsChart(
    dateRange: DashboardStatsViewModel.DateRangeState?,
    revenueStatsState: DashboardStatsViewModel.RevenueStatsViewState?,
    visitorsStatsState: DashboardStatsViewModel.VisitorStatsViewState?,
    lastUpdateState: Long?,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onAddCustomRangeClick: () -> Unit,
    onChartDateSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    val context = LocalContext.current

    val statsView = remember(context) {
        DashboardStatsView(context).apply {
            initView(
                dateUtils = dateUtils,
                currencyFormatter = currencyFormatter,
                usageTracksEventEmitter = usageTracksEventEmitter,
                lifecycleScope = lifecycleScope,
                onViewAnalyticsClick = {},
                onDateSelected = onChartDateSelected
            )
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            statsView.apply {
                customRangeButton.setOnClickListener { onAddCustomRangeClick() }
                customRangeLabel.setOnClickListener { onAddCustomRangeClick() }
            }
        }
    )

    // Update the view using side effects
    // This is better than using [AndroidView]'s update because it allows for granular updates, while the former
    // is applying all properties on each composition (even the unchanged ones) which creates issues with the legacy
    // view.

    LaunchedEffect(dateRange?.rangeSelection) {
        dateRange?.rangeSelection?.let { statsView.loadDashboardStats(it) }
    }

    LaunchedEffect(lastUpdateState) {
        statsView.showLastUpdate(lastUpdateState)
    }

    LaunchedEffect(revenueStatsState) {
        when (revenueStatsState) {
            is DashboardStatsViewModel.RevenueStatsViewState.Content -> {
                statsView.showErrorView(false)
                statsView.showSkeleton(false)
                statsView.updateView(revenueStatsState.revenueStats)
            }

            DashboardStatsViewModel.RevenueStatsViewState.GenericError -> {
                statsView.showErrorView(true)
                statsView.showSkeleton(false)
            }

            is DashboardStatsViewModel.RevenueStatsViewState.Loading -> {
                statsView.showErrorView(false)
                statsView.showSkeleton(true)
                if (revenueStatsState.isForced) {
                    statsView.clearStatsHeaderValues()
                    statsView.clearChartData()
                }
            }

            else -> Unit
        }
    }

    LaunchedEffect(visitorsStatsState) {
        visitorsStatsState?.let {
            statsView.showVisitorStats(it)
        }
    }
}

@Composable
private fun HandleEvents(
    event: LiveData<MultiLiveEvent.Event>,
    openDatePicker: (Long, Long) -> Unit,
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: MultiLiveEvent.Event ->
            when (event) {
                is DashboardStatsViewModel.OpenDatePicker -> {
                    openDatePicker(event.fromDate.time, event.toDate.time)
                }

                is DashboardStatsViewModel.OpenAnalytics -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToAnalytics(event.analyticsPeriod)
                    )
                }
            }
        }

        event.observe(lifecycleOwner, observer)

        onDispose {
            event.removeObserver(observer)
        }
    }
}
