package com.woocommerce.android.ui.dashboard.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.JetpackBenefitsBannerUiModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun DashboardStatsCard(
    dateUtils: DateUtils,
    currencyFormatter: CurrencyFormatter,
    usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
    onPluginUnavailableError: () -> Unit,
    reportJetpackPluginStatus: (JetpackBenefitsBannerUiModel?) -> Unit,
    onStatsError: () -> Unit,
    openDatePicker: (Long, Long, (Long, Long) -> Unit) -> Unit,
    viewModel: DashboardStatsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
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

    LaunchedEffect(visitorsStatsState) {
        reportJetpackPluginStatus(
            (visitorsStatsState as? DashboardStatsViewModel.VisitorStatsViewState.Unavailable)?.benefitsBanner
        )
    }

    HandleEvents(
        event = viewModel.event,
        openDatePicker = { fromDate, toDate ->
            openDatePicker(fromDate, toDate) { from, to ->
                viewModel.onCustomRangeSelected(StatsTimeRange(Date(from), Date(to)))
            }
        }
    )

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

@Composable
fun HandleEvents(
    event: LiveData<Event>,
    openDatePicker: (Long, Long) -> Unit,
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(event, navController, lifecycleOwner) {
        event.observe(lifecycleOwner) { event ->
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
    val coroutineScope = rememberCoroutineScope()

    AndroidView(
        factory = { context ->
            DashboardStatsView(context).apply {
                initView(
                    dateUtils = dateUtils,
                    currencyFormatter = currencyFormatter,
                    usageTracksEventEmitter = usageTracksEventEmitter,
                    lifecycleScope = lifecycleScope,
                    onViewAnalyticsClick = onViewAnalyticsClick
                )

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
        },
        update = { view ->
            selectedDateRange?.let { view.loadDashboardStats(it) }
            if (view.tabLayout.getTabAt(view.tabLayout.selectedTabPosition)?.tag != selectedDateRange?.selectionType) {
                val index = (0..view.tabLayout.tabCount)
                    .firstOrNull { view.tabLayout.getTabAt(it)?.tag == selectedDateRange?.selectionType }
                index?.let {
                    coroutineScope.launch {
                        // Small delay needed to ensure tablayout scrolls to the selected tab if tab is not visible on screen.
                        delay(300)
                        view.tabLayout.getTabAt(index)?.select()
                    }
                }
            }

            view.handleCustomRangeTab(customRange)

            view.showLastUpdate(lastUpdateState)

            when (revenueStatsState) {
                is DashboardStatsViewModel.RevenueStatsViewState.Content -> {
                    view.showErrorView(false)
                    view.showSkeleton(false)
                    view.tabLayout.isVisible = AppPrefs.isV4StatsSupported()
                    view.updateView(revenueStatsState.revenueStats)
                }

                DashboardStatsViewModel.RevenueStatsViewState.GenericError -> {
                    view.showErrorView(true)
                    view.showSkeleton(false)
                }

                DashboardStatsViewModel.RevenueStatsViewState.Loading -> {
                    view.showErrorView(false)
                    view.showSkeleton(true)
                }

                else -> Unit
            }

            visitorsStatsState?.let {
                view.showVisitorStats(it)
            }
        }
    )
}
