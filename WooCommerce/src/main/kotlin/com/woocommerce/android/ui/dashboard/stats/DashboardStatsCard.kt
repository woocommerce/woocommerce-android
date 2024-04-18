package com.woocommerce.android.ui.dashboard.stats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun DashboardStatsCard(
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onPluginUnavailableError: () -> Unit,
    onStatsError: () -> Unit,
    openDatePicker: (Long, Long, (Long, Long) -> Unit) -> Unit,
    parentViewModel: DashboardViewModel,
    viewModel: DashboardStatsViewModel = viewModelWithFactory<DashboardStatsViewModel, DashboardStatsViewModel.Factory>(
        creationCallback = {
            it.create(parentViewModel)
        }
    )
) {
    val customRange by viewModel.customRange.observeAsState()
    val selectedDateRange by viewModel.selectedDateRange.observeAsState()
    val revenueStatsState by viewModel.revenueStatsState.observeAsState()
    val visitorsStatsState by viewModel.visitorStatsState.observeAsState()
    val lastUpdateState by viewModel.lastUpdateStats.observeAsState()

    LaunchedEffect(revenueStatsState) {
        when (revenueStatsState) {
            is DashboardStatsViewModel.RevenueStatsViewState.GenericError -> onStatsError()
            is DashboardStatsViewModel.RevenueStatsViewState.PluginNotActiveError -> onPluginUnavailableError()
            else -> Unit
        }
    }

    HandleEvents(
        event = viewModel.event,
        openDatePicker = { fromDate, toDate ->
            openDatePicker(fromDate, toDate) { from, to ->
                viewModel.onCustomRangeSelected(StatsTimeRange(Date(from), Date(to)))
            }
        }
    )

    WidgetCard(
        titleResource = R.string.my_store_widget_stats_title,
        menu = DashboardViewModel.DashboardWidgetMenu(
            items = listOf(
                DashboardViewModel.DashboardWidgetAction(
                    titleResource = R.string.dynamic_dashboard_hide_widget_menu_item,
                    action = { /* TODO */ }
                )
            )
        )
    ) {
        DashboardStatsCard(
            selectedDateRange = selectedDateRange,
            customRange = customRange,
            revenueStatsState = revenueStatsState,
            visitorsStatsState = visitorsStatsState,
            lastUpdateState = lastUpdateState,
            dateUtils = dateUtils,
            currencyFormatter = currencyFormatter,
            usageTracksEventEmitter = usageTracksEventEmitter,
            onViewAnalyticsClick = viewModel::onViewAnalyticsClicked,
            onAddCustomRangeClick = viewModel::onAddCustomRangeClicked,
            onTabSelected = viewModel::onTabSelected
        )
    }
}

@Composable
fun DashboardStatsCard(
    selectedDateRange: StatsTimeRangeSelection?,
    customRange: StatsTimeRange?,
    revenueStatsState: DashboardStatsViewModel.RevenueStatsViewState?,
    visitorsStatsState: DashboardStatsViewModel.VisitorStatsViewState?,
    lastUpdateState: Long?,
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onViewAnalyticsClick: () -> Unit,
    onAddCustomRangeClick: () -> Unit,
    onTabSelected: (SelectionType) -> Unit,
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
                onViewAnalyticsClick = onViewAnalyticsClick
            )
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = {
            statsView.apply {
                customRangeButton.setOnClickListener { onAddCustomRangeClick() }
                customRangeLabel.setOnClickListener { onAddCustomRangeClick() }

                tabLayout.addOnTabSelectedListener(
                    object : TabLayout.OnTabSelectedListener {
                        override fun onTabSelected(tab: TabLayout.Tab) {
                            onTabSelected(tab.tag as? SelectionType ?: SelectionType.TODAY)
                        }

                        override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                        override fun onTabReselected(tab: TabLayout.Tab) = Unit
                    }
                )
            }
        }
    )

    // Update the view using side effects
    // This is better than using [AndroidView]'s update because it allows for granular updates, while the former
    // is applying all properties on each composition (even the unchanged ones) which creates issues with the legacy
    // view.

    LaunchedEffect(customRange) {
        statsView.handleCustomRangeTab(customRange)
    }

    LaunchedEffect(selectedDateRange) {
        selectedDateRange?.let { statsView.loadDashboardStats(it) }
        val selectionType = selectedDateRange?.selectionType
        if (statsView.tabLayout.getTabAt(statsView.tabLayout.selectedTabPosition)?.tag != selectionType) {
            val index = (0..statsView.tabLayout.tabCount)
                .firstOrNull { statsView.tabLayout.getTabAt(it)?.tag == selectionType }
            index?.let {
                launch {
                    // Small delay needed to ensure tablayout scrolls to the selected tab if tab is not visible on screen.
                    delay(300)
                    statsView.tabLayout.getTabAt(index)?.select()
                }
            }
        }
    }

    LaunchedEffect(lastUpdateState) {
        statsView.showLastUpdate(lastUpdateState)
    }

    LaunchedEffect(revenueStatsState) {
        when (revenueStatsState) {
            is DashboardStatsViewModel.RevenueStatsViewState.Content -> {
                statsView.showErrorView(false)
                statsView.showSkeleton(false)
                statsView.tabLayout.isVisible = AppPrefs.isV4StatsSupported()
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
